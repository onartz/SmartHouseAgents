package test;


import jade.content.ContentElement;
import jade.content.abs.AbsContentElement;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.OntologyException;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.gui.GuiAgent;
import jade.gui.GuiEvent;
import jade.lang.acl.ACLMessage;
import jade.proto.SubscriptionInitiator;
import jade.util.leap.Iterator;
import meteo.Ontology;
import meteo.onto.Alert;
import meteo.onto.HasLevel;

import meteo.onto.StrongWind;
import meteo.onto.Wind;


/*
 * Takes one mandatory argument : agent name
 */

public class MeteoTestAg extends GuiAgent{

	private SLCodec langage;
	private ACLMessage msg;
	//private ACLMessage df_subscribe_msg;

	@SuppressWarnings("serial")
	@Override
	protected void setup() {
		// TODO Auto-generated method stub
		super.setup();
		
		Object[] args = getArguments();
		if(args.length<1){
			//TODO : Kill
		}
		//MeteoAgent name
		String name = (String)args[0];
		
		//Register language and onto in content manager
		langage = new SLCodec();
		getContentManager().registerLanguage(new SLCodec());
		getContentManager().registerOntology(Ontology.getInstance());
		
		msg = new ACLMessage(ACLMessage.SUBSCRIBE);
		AID meteoAg = new AID(name, AID.ISLOCALNAME);		
		
		msg.addReceiver(meteoAg);
		msg.setLanguage(langage.getName());
		msg.setOntology(Ontology.NAME);
		
		
		
		try {
			//Subscribe to Wind changes
			HasLevel content = new HasLevel();
			getContentManager().fillContent(msg, content);
			//frame.appendLog(msg.toString(), false);
			addBehaviour(new SubscriptionInitiator(this, msg) {

				@Override
				protected void handleInform(ACLMessage msg) {
					
					ContentElement reply;
					AID sender = msg.getSender();
					try {
						reply = getContentManager().extractContent(msg);
						if(reply instanceof HasLevel){
							System.out.print(sender.getName());
							System.out.print(" said : actual wind is : ");
							System.out.println(((HasLevel) reply).getWind().getClass());
							
							/*if(((HasLevel)reply).getWind() instanceof StrongWind ){
							System.out.print(" said : actual wind is : ");
							//System.out.println(((Wind)reply).getSpeed());
							}
							else
								System.out.println(((HasLevel) reply).getWind().getSpeed());*/
											
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

	}

	@Override
	protected void takeDown() {
		// TODO Auto-generated method stub
		super.takeDown();
	}

	@Override
	protected void onGuiEvent(GuiEvent arg0) {
		// TODO Auto-generated method stub
		
		
	}

}
