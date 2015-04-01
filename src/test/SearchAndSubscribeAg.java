package test;



import meteo.onto.IsStrong;
import jade.content.ContentElement;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.OntologyException;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.proto.SubscriptionInitiator;
import jade.util.leap.Iterator;

public class SearchAndSubscribeAg extends Agent{
	
	SLCodec langage = new SLCodec();

	
	
	
	@Override
	protected void setup() {
		langage = new SLCodec();
		getContentManager().registerLanguage(new SLCodec());
		getContentManager().registerOntology(meteo.Ontology.getInstance());
		Agent myAgent = this;
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription templateSd = new ServiceDescription();
		templateSd.setType("meteo");
		templateSd.addProperties(new Property("location", "Benney"));
		template.addServices(templateSd);
		SearchConstraints sc = new SearchConstraints();
		
	/*	SearchConstraints sc = new SearchConstraints();
		sc.setMaxResults((long) 2);
		
		try {
			DFAgentDescription[] results = DFService.search(this, template, sc);
			
			if(results.length > 0){
				System.out.println("Agent " + this.getName() + " has found this " + templateSd.getName() + "Service(s)");
				for(int i = 0; i<results.length;i++)
				{
					DFAgentDescription dfd = results[i];
					AID provider = dfd.getName();
					Iterator it = dfd.getAllServices();
					while(it.hasNext()){
						ServiceDescription sd = (ServiceDescription)it.next();
						if(sd.getType().equals("meteo"))
						{
							System.out.println("- Service : " + sd.getName() + " provided by agent " + provider.getLocalName());
							System.out.println("Let subscribe....");
							
							//subscribe(provider.getLocalName());
							
						}						
					}				
				}
			}
		} catch (FIPAException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		/*
		 * Subsribe to new services
		 * Receive notfication when a new service is proposed by an agent
		 */
		 addBehaviour(new SubscriptionInitiator(this, DFService.createSubscriptionMessage(this, getDefaultDF(), template, sc)) {
	            protected void handleInform(ACLMessage inform) {
	            System.out.println("Agent "+getLocalName()+": Notification received from DF");
	            try {
	                    DFAgentDescription[] results = DFService.decodeNotification(inform.getContent());
	                if (results.length > 0) {
	                    for (int i = 0; i < results.length; ++i) {
	                        DFAgentDescription dfd = results[i];
	                        AID provider = dfd.getName();
	                        // The same agent may provide several services; we are only interested
	                        // in the weather-forcast one
	                        Iterator it = dfd.getAllServices();
	                        while (it.hasNext()) {
	                            ServiceDescription sd = (ServiceDescription) it.next();
	                            if (sd.getType().equals("meteo")) {
	                                System.out.println("meteo service for Benney found:");
	                                System.out.println("- Service \""+sd.getName()+"\" provided by agent "+provider.getName());
	                                System.out.println("Let subscribe....");
	    							
	    							subscribe(provider.getLocalName());
	                            }
	                        }
	                    }
	                }   
	                System.out.println();
	            }
	            catch (FIPAException fe) {
	                fe.printStackTrace();
	            }
	            }
	        } );
		
	
		
	}

	private void subscribe(String localName) {
		// TODO Auto-generated method stub
		ACLMessage msg = new ACLMessage(ACLMessage.SUBSCRIBE);
		AID meteoAg = new AID(localName, AID.ISLOCALNAME); 
		msg.addReceiver(meteoAg);
		msg.setLanguage(langage.getName());
		msg.setOntology(meteo.Ontology.NAME);
		IsStrong  content= new IsStrong();
		//content.setInventory(new Inventory(inventoryAg));
		//content.setQuantity(10);
		//id = contained_product_ids.get(name);
		//content.setProduct(new Product(id));
		try {
			getContentManager().fillContent(msg, content);
			
			addBehaviour(new SubscriptionInitiator(this, msg) {

				@Override
				protected void handleInform(ACLMessage msg) {
					
					
					ContentElement reply;
					try {
						reply = getContentManager().extractContent(msg);
					
						if (reply instanceof IsStrong) {
							System.out.println("Alert wind : ");
						}
					} catch (CodecException | OntologyException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
			});
		} catch (CodecException | OntologyException e) {			
			e.printStackTrace();
		}
		// TODO Auto-generated method stub
		
	}

	private void subscribe() {
		
		
	}

	@Override
	protected void takeDown() {
		// TODO Auto-generated method stub
		super.takeDown();
	}
	

}