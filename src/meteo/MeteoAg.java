package meteo;

import java.io.IOException;
import java.io.InputStream;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Map.Entry;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import meteo.Ontology;
import meteo.onto.Alert;
import meteo.onto.HasLevel;
import meteo.onto.IsStrong;
import meteo.onto.Rain;
import meteo.onto.Strong;
import meteo.onto.WeatherPhenomenon;
import meteo.onto.Wind;
import meteo.onto.WindLevel;
import jade.content.ContentElement;
import jade.content.Predicate;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.OntologyException;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.SubscriptionResponder;
import jade.proto.SubscriptionResponder.Subscription;
import jade.proto.SubscriptionResponder.SubscriptionManager;
import jade.util.leap.Iterator;

/*
 * An agent providing actuel weather informations (meteoSet) and forecasts from location (longitude, latitude)
 * a meteoSet is composed of : wind, humidity, temperature
 * This agent publishes a meteoService to other agents
 * This agent 
 * Takes 5 mandatory arguments :
 * longitude, latitude, serviceName, low and high TemperatureAlert
 * this agent reponds to this requests :
 * SUBSCRIBE : notify if wind > alertValue
 * QUERY_REF : ...
 * 
 * Subscribers are notified when dangerous phenomena starts and end
 * If dangerous phenomenous ha started when agent subscribes, it is notified
 */

/*
 * Cet agent publie un service de meteo dans le DF lors de changement de niveau
 * Les niveaux concernent les températures et le vent
 * Ils sont décrits dans l'ontologie utilisée
 * 
 * D'autres agents  peuvent découvrir ce service et s'abonner
 * 
 * Un agent envoie une requete de SUBSCRIBE
 * Cet agent ajoute le demandeur dans sa liste de subscribers
 * Cet agent répond par un message INFORM
 * 
 * Par exemple : un agent volet roulant est averti en cas de vent fort, ce qui lui permet de commander
 * la fermeture du volet.
 * 
 */

/*
 * Todo : gerer les subscribers qui meurent
 */
public class MeteoAg extends Agent{
	
	private static final int METEO_PORT = 80;
	private static final String METEO_HOST="api.openweathermap.org";
	private static final String METEO_REQUEST_BASE = "/data/2.5/weather?units=metric";
	private static final long serialVersionUID = 1L;
	private static final int INFLUXDB_PORT = 8086;
	private static final String INFLUXDB_HOST = "193.55.104.132";
	private static final String INFLUXDB_REQUEST_BASE = "/db/";
	private static final String INFLUXDB_USERNAME = "root";
	private static final String INFLUXDB_USERPASSWORD = "root";
	private static final String INFLUXDB_DATABASENAME = "Loumanolkar";
	//strongWind = windSpeed > 2m/s
	//private static final int strongWindTreshold = 2;
	//public static final float strongWindThreshold = 1;
	/*
	 * 
	 * Agent params
	 */
	private float agLongitude;
	private float agLatitude;
	//private boolean windSpeedAlert;
	private float highTemperatureAlert;
	private float lowTemperatureAlert;
	
	
	/*
	 * A subscriber can subscribe via a subscription which refers to one type
	 */
	@SuppressWarnings("unused")
	private class subscriptionParams{
		private boolean lastState;
		private WeatherPhenomenon weatherPhenomenon;

		public subscriptionParams(boolean lastState,
				WeatherPhenomenon weatherPhenomenon) {
			super();
			this.lastState = lastState;
			this.weatherPhenomenon = weatherPhenomenon;
		}

		public boolean isLastState() {
			return lastState;
		}

		public void setLastState(boolean lastState) {
			this.lastState = lastState;
		}

		public WeatherPhenomenon getWeatherPhenomenon() {
			return weatherPhenomenon;
		}

		public void setWeatherPhenomenon(WeatherPhenomenon weatherPhenomenon) {
			this.weatherPhenomenon = weatherPhenomenon;
		}		
	}
	
	/*
	 * To maintain a list of subscribers
	 */
	private Map<Subscription, subscriptionParams> subscribers;
	


	/*
	 * Agent constructor
	 */
	public MeteoAg() {
		super();
		model = new MeteoModel();
		subscribers = new HashMap<Subscription, subscriptionParams>();
	}
		
	/*Represents one meteoSet */
	public class MeteoSet{
		
		public MeteoSet(){
			temperature = 0;
			humidity = 0;
			wind = new Wind();		
		}
		/* update existing meteoSet with values 
		 * 
		 */
		public void update(float temperature, int humidity, float windSpeed){
			this.temperature = temperature;
			this.humidity = humidity;
			wind.setSpeed(windSpeed);		
		}

		public void update(float temperature, int humidity, Wind wind){
			this.temperature = temperature;
			this.humidity = humidity;
			this.setWind(wind);		
			
		}
		public float getTemperature() {
			return temperature;
		}
		public void setTemperature(float temperature) {
			this.temperature = temperature;
		}
		public int getHumidity() {
			return humidity;
		}
		public void setHumidity(int humidity) {
			this.humidity = humidity;
		}
		public Wind getWind() {
			return wind;
		}
		public void setWind(Wind wind) {
			this.wind = wind;
		}
		private float temperature;
		private int humidity;
		private Wind wind;
		
	}
	
	/*
	 * Represents a meteo forecast eg a list of MeteoSet at one time (+3, +6 etc)
	 */
	class MeteoForecast {
		private ArrayList<MeteoSet> forecasts;
		public MeteoForecast(){
			forecasts = new ArrayList<MeteoSet>();
					
		}
		
		
	} 
	
	class MeteoModel extends Observable{
		/*
		 * Does the wind level have changed since last time?
		 */
		public boolean windLevelHasChanged;
		
		public void setWindLevelHasChanged(boolean windLevelHasChanged) {
			this.windLevelHasChanged = windLevelHasChanged;
		}
		public boolean isWindLevelHasChanged() {
			return windLevelHasChanged;
		}


		//Meteo de maintenant
		private MeteoSet actualMeteoSet;
		public MeteoSet getActual() {
			return actualMeteoSet;
		}
		public void setActual(MeteoSet actualMeteoSet) {
			this.actualMeteoSet = actualMeteoSet;
		}
		
		
		//Prevision
		private MeteoForecast forecast;
		
		private AID agentAid;
		//private float temperature;
		//private float wind;
		private float latitude;
		private float longitude;
		//private int humidity;
		//Threshold for StrongWind
		
		private boolean lowTemperatureAlert;
		private boolean highTemperatureAlert;
		private float lowTemperature;
		private float highTemperature;
		private boolean fmWindSpeedAlert;
		private boolean fdWindSpeedAlert;
		private boolean fmHighTemperatureAlert;
		private boolean fmLowTemperatureAlert;
		private boolean fdHighTemperatureAlert;
		private boolean fdLowTemperatureAlert;
		
		//private Wind wind;
		
		
		public void setWind(Wind wind) {
			this.actualMeteoSet.setWind(wind);
		}
		
		public float getWindSpeed() {
			return windSpeed;
		}
		public void setWindSpeed(float windSpeed) {
			this.windSpeed = windSpeed;
		}
		private float windSpeed;
		private boolean windSpeedAlert;

		private MeteoSet previousMeteoSet;
		
		public boolean isFmWindSpeedAlert() {
			return fmWindSpeedAlert;
		}
		public void setFmWindSpeedAlert(boolean fmWindSpeedAlert) {
			this.fmWindSpeedAlert = fmWindSpeedAlert;
		}
		public boolean isFdWindSpeedAlert() {
			return fdWindSpeedAlert;
		}
		public void setFdWindSpeedAlert(boolean fdWindSpeedAlert) {
			this.fdWindSpeedAlert = fdWindSpeedAlert;
		}
		
		
		public float getLatitude() {
			return latitude;
		}
		public void setLatitude(float latitude) {
			this.latitude = latitude;
		}
		public float getLongitude() {
			return longitude;
		}
		public void setLongitude(float longitude) {
			this.longitude = longitude;
		}
		public AID getAgentAid() {
			return agentAid;
		}
		public void setAgentAid(AID agentAid) {
			this.agentAid = agentAid;
		}
		

		public void setAgentID(AID aid) {
			// TODO Auto-generated method stub
			
		}
		public boolean isWindSpeedAlert() {
			return windSpeedAlert;
		}
		public boolean isHighTemperatureAlert() {
			
			return highTemperatureAlert;
		}
		public boolean isLowTemperatureAlert() {
			// TODO Auto-generated method stub
			return lowTemperatureAlert;
		}
		/*
		 * Default constructor
		 */
		public MeteoModel() {
			super();
			longitude = 0;
			latitude = 0;
			forecast = new MeteoForecast();
			actualMeteoSet = new MeteoSet();
			previousMeteoSet = new MeteoSet();
			windLevelHasChanged = true;
			
		}
		public void update(String jsonResult) {
			
			try {
				JSONObject jsonObj = new JSONObject(jsonResult);
			
					float temperature = Float.parseFloat(jsonObj.getJSONObject("main").get("temp").toString());
					
					DecimalFormat df = new DecimalFormat("00.00");
					df.setRoundingMode(RoundingMode.HALF_UP);
					temperature = Float.parseFloat(df.format(temperature));	
					
					//DecimalFormat df = new DecimalFormat("00.00");
					//df.setRoundingMode(RoundingMode.HALF_UP);
					int humidity = Integer.parseInt(jsonObj.getJSONObject("main").get("humidity").toString());
					//humidity = Float.parseFloat(df.format(temperature));	
					
					float windSpeedValue = Float.parseFloat(jsonObj.getJSONObject("wind").get("speed").toString());
					df.applyPattern("000.00");
					windSpeedValue = Float.parseFloat(df.format(windSpeedValue));
									
					//udate with new datas
					//Todo : changer windDirection
					Wind wind = new Wind(windSpeedValue,0);
					
					/*if(!(wind.getWindLevel().getClass().equals(actualMeteoSet.getWind().getWindLevel().getClass())))
							model.setWindLevelHasChanged(true);*/
					actualMeteoSet.update(temperature, humidity, wind);
													
				
					//actualMeteoSet.update(temperature, humidity, new Wind(windSpeedValue, 0));
					System.out.print("Temperature : ");System.out.println(actualMeteoSet.getTemperature());
					System.out.print("WindSpeedValue : ");System.out.println(actualMeteoSet.getWind().getSpeed());
					
					System.out.print("Number of windSubscribers : ");System.out.println(subscribers.size());
					
					//if(previousMeteoSet.getwin)
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			setChanged();
			notifyObservers();				
		}	
	}
	
	MeteoModel model;
	private String serviceName = null;
	
	
	
	private void queryMeteoServer(){
		CloseableHttpClient client = HttpClientBuilder.create().build();
		String jsonResult = null;
		try{
		HttpHost target = new HttpHost(METEO_HOST, METEO_PORT, "http");
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(METEO_REQUEST_BASE);
		stringBuilder.append("&lon=");
		stringBuilder.append(agLongitude);
		stringBuilder.append("&lat=");
		stringBuilder.append(agLatitude);
		// specify the get request
		String request = stringBuilder.toString();
		HttpGet getRequest = new HttpGet(request);
		System.out.println("executing request to " + target);
		System.out.println("executing  " + request);
		// TODO: set timeout
		HttpResponse httpResponse = client.execute(target, getRequest);
		HttpEntity entity = httpResponse.getEntity();
/*
		System.out.println("----------------------------------------");
		System.out.println(httpResponse.getStatusLine());
		Header[] headers = httpResponse.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			System.out.println(headers[i]);
		}
		System.out.println("----------------------------------------");*/

		if (entity != null) {
			jsonResult = (EntityUtils.toString(entity));
			//System.out.println(jsonResult);
		}

		
		// TODO : modify model
		model.update(jsonResult);

	} catch (Exception e) {
		e.printStackTrace();
	} finally {
		// When HttpClient instance is no longer needed,
		// shut down the connection manager to ensure
		// immediate deallocation of all system resources
		try {
			client.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}	
	}

	

	@Override
	protected void setup() {
		// TODO Auto-generated method stub
		super.setup();
		// Register language and onto in content manager
		SLCodec langage = new SLCodec();
		getContentManager().registerLanguage(new SLCodec());
		getContentManager().registerOntology(Ontology.getInstance());

		model.setAgentID(getAID());
		
		Object[] args = getArguments();
		if(args.length<6){
			//TODO : Kill
		}
		
		agLongitude = Float.parseFloat((String) args[0]);
		agLatitude = Float.parseFloat((String) args[1]);
		serviceName = (String)args[2];
		lowTemperatureAlert = Float.parseFloat((String)args[3]);
		highTemperatureAlert = Float.parseFloat((String)args[4]);
		//windSpeedAlert = Float.parseFloat((String)args[5]);
				
		model.setLongitude(agLongitude);
		model.setLatitude(agLatitude);
		//Thresholds
		//model.setWindSpeed(strongWindThreshold);
		//model.setLowTemperature(lowTemperatureAlert);
		//model.setHighTemperature(highTemperatureAlert);
		queryMeteoServer();	
		logWeatherData();
		
		// Register the service in DF
	  	System.out.println("Agent "+getLocalName()+" registering service \""+serviceName+"\" of type \"meteo\"");
	  	try {
	  		DFAgentDescription dfd = new DFAgentDescription();
	  		dfd.setName(getAID());
	  		ServiceDescription sd = new ServiceDescription();
	  		sd.setName(serviceName);
	  		sd.setType("meteo");
	  		// Agents that want to use this service need to "know" the meteo-ontology
	  		sd.addOntologies("meteo-ontology");
	  		// Agents that want to use this service need to "speak" the FIPA-SL language
	  		sd.addLanguages(FIPANames.ContentLanguage.FIPA_SL);
	  		// Properties for this instance
	  		sd.addProperties(new Property("location", "Benney"));
	  		dfd.addServices(sd);
	  		
	  		DFService.register(this, dfd);
	  	}
	  	catch (FIPAException fe) {
	  		fe.printStackTrace();
	  	}
	  	
	  	
		model.addObserver(new Observer(){
			
			/*
			 * On Every updates, try to see if there is an observer to notify (depending on threshold)
			 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
			 */		
			@Override
			/*
			 * TODO : Only works for wind. Modify to works with multiple threshold(non-Javadoc)
			 */
			public void update(Observable arg0, Object arg1) {
				for (Entry<Subscription, subscriptionParams> subscriber : subscribers
						.entrySet()) {
					if(subscriber.getValue().getWeatherPhenomenon() instanceof Wind)
					{			
						if(model.isWindLevelHasChanged())
								
							try {
								notifySubscriber(subscriber.getKey());
								} catch (CodecException | OntologyException e) {
								// TODO Auto-generated catch block
									e.printStackTrace();
							}
						}						
					
					else if(subscriber.getValue().getWeatherPhenomenon() instanceof Rain)
						{
						}
				}	
				//observers have been notified, lets reset the flag
				if(model.isWindLevelHasChanged())
					model.setWindLevelHasChanged(false);
				//TODO : idem for Rain
			}
	
		});

		/*
		 * Récupération des infos météo sur openweathermap, toutes les xx sec
		 * mise à jour du modele
		 */
		addBehaviour(new TickerBehaviour(this,10000){

			@Override
			protected void onTick() {
				// TODO Auto-generated method stub
				//System.out.print("Top");
				queryMeteoServer();
				logWeatherData();
				
			}		
		});
		//Responder to Query-Ref request
		//MessageTemplate mt1 = MessageTemplate.MatchPerformative(ACLMessage.QUERY_REF);
		
		
		// Responder to subscription Request
				MessageTemplate sub_mt = MessageTemplate
						.MatchPerformative(ACLMessage.SUBSCRIBE);
				SubscriptionManager sm = new SubscriptionResponder.SubscriptionManager() {

					@Override
					public boolean register(Subscription s) throws RefuseException,
							NotUnderstoodException {
						try {
							ACLMessage m = s.getMessage();
							ContentElement content = getContentManager().extractContent(m);
							//Subscription to WindLevel
							if(content instanceof HasLevel){
								subscribers.put(s,
										new subscriptionParams(false, new Wind()));
								notifySubscriber(s);
														
							} else {
								throw new NotUnderstoodException("not understood");
							}
						} catch (CodecException | OntologyException e) {

						}
						return true;
					}

					@Override
					public boolean deregister(Subscription s) throws FailureException {
						subscribers.remove(s);
						return false;
					}
				};
				addBehaviour(new SubscriptionResponder(this, sub_mt, sm));

	
	}
	/*
	 * Method to log weather datas in InfluxDB
	 */
	private void logWeatherData() {
		CloseableHttpClient client = HttpClientBuilder.create().build();
		
		try{
			HttpHost target = new HttpHost(INFLUXDB_HOST, INFLUXDB_PORT, "http");
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(INFLUXDB_REQUEST_BASE);
			stringBuilder.append(INFLUXDB_DATABASENAME);
			stringBuilder.append("/series?u=");
			stringBuilder.append(INFLUXDB_USERNAME);
			stringBuilder.append("&p=");
			stringBuilder.append(INFLUXDB_USERPASSWORD);
			// specify the get request
			String request = stringBuilder.toString();
			
			/*
			 * String in the Post. InfluxDB Compliant
			 */
			StringBuilder data = new StringBuilder();
			data.append("[{\"name\" : \"ExtTemperature\", \"columns\" : [\"value\"], \"points\" : [[");
			data.append(model.getActual().getTemperature());
			//data.append(model.get)
			data.append("]]},{\"name\" : \"Humidity\", \"columns\" : [\"value\"], \"points\" : [[");
			data.append(model.getActual().getHumidity());
			data.append("]]},{\"name\" : \"Wind\", \"columns\" : [\"value\"], \"points\" : [[");
			data.append(model.getActual().getWind().getSpeed());
			data.append("]]}]");
			System.out.println(data.toString());
		
			StringEntity params = new StringEntity(data.toString());
			
			HttpPost req = new HttpPost(request);	
					  
		    req.addHeader("content-type", "application/json");
		    req.setEntity(params);
		    
		    System.out.println("executing request to " + target);
			System.out.println("executing  " + request);
			
		    HttpResponse httpResponse = client.execute(target, req);			
		    
			/*System.out.println("----------------------------------------");
			
			System.out.println(httpResponse.getStatusLine());
			Header[] headers = httpResponse.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				System.out.println(headers[i]);
			}
			System.out.println("----------------------------------------");
			InputStream body = httpResponse.getEntity().getContent();
			System.out.println(body.toString());*/		
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// When HttpClient instance is no longer needed,
			// shut down the connection manager to ensure
			// immediate deallocation of all system resources
			try {
				client.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	

	/*
	 * Notify subscribers when wind exceeds speed limit
	 * 
	 */
	protected void notifySubscriber(Subscription s) throws CodecException, OntologyException {
		
		ACLMessage notification = s.getMessage().createReply();
		notification.setPerformative(ACLMessage.INFORM);
		WeatherPhenomenon wp = subscribers.get(s).getWeatherPhenomenon();
		Predicate reply = null;
		if(wp instanceof Wind){
			reply = new HasLevel();
			((HasLevel) reply).setWind(model.getActual().getWind());
			((HasLevel) reply).setWindLevel(model.getActual().getWind().getWindLevel());
			
		}
		else if(wp instanceof Rain){
			//TODO : treat rain
		}
	
		
		
		//reply.setWindLevel(new Strong());
		//reply.setWind(model.getActual().getWind());
		//IsStrong reply = new IsStrong();
		//Alert reply = new Alert();
		//JSONObject main = new JSONObject();
		//JSONObject obj = new JSONObject();
		
		//StringBuilder sb;
		/*if(model.isFmWindSpeedAlert())
		{*/
			//reply.setWind(model.getActual().getWind());
			//reply.setSpeed(model.getWindSpeed());
			
			
			/*WindAlert windAlert = reply.new WindAlert();
			windAlert.setStatus("Start");
			windAlert.setValue(model.getWind());*/
			//reply.setWindAlert(windAlert);
			/*obj.put("Status", "Start");
			obj.put("Speed", model.getWind());
			main.put("WindAlert", obj);*/
			//System.out.println(main.toString());
			
			//obj.put(key, value)
			/*sb = new StringBuilder();
			sb.append("Alerte vent violent : ");
			sb.append(model.getWind());*/
	
			/*notification.setContent(main.toString());
			s.notify(notification);*/
		//}
		/*else if(model.isFdWindSpeedAlert())
		{
			WindAlert windAlert = reply.new WindAlert();
			windAlert.setStatus("End");
			windAlert.setValue(model.getWind());
			
		}*/
		
		/*if(model.isFmHighTempAlert())
		{
			TemperatureAlert temperatureAlert = reply.new TemperatureAlert("Start","High",model.getTemperature());
			reply.setTempAlert(temperatureAlert);
			
		}
		else if(model.isFdHighTempAlert())
		{
			TemperatureAlert temperatureAlert = reply.new TemperatureAlert("End","High",model.getTemperature());
			reply.setTempAlert(temperatureAlert);
		}
		
		if(model.isFmLowTempAlert())
		{
			TemperatureAlert temperatureAlert = reply.new TemperatureAlert("Start","Low",model.getTemperature());
			reply.setTempAlert(temperatureAlert);
		}
		else if(model.isFdLowTempAlert())
		{
			TemperatureAlert temperatureAlert = reply.new TemperatureAlert("End","Low",model.getTemperature());
			reply.setTempAlert(temperatureAlert);
		}*/
		getContentManager().fillContent(notification, reply);
		
	
		s.notify(notification);
		
		
			
		
		/*
		 * TODO : Only for wind limit
		 */
		//ExceedsWindLimit reply = new ExceedsWindLimit();
		//reply.setValue(model.getWind());
				
		//getContentManager().fillContent(notification, reply);
		
		
		//Threshold thr = subscribers.get(s);
		//thr.lastState = (thr.limit >= model.getWind());			
		
	}
	
	/**
	 * Cleanly exit agent
	 */
	@Override
	protected void takeDown() {
		// Close GUI
		//frame.dispose();
		// De-register DF
		try {
			DFService.deregister(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	
}
