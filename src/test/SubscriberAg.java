package test;

import jade.content.lang.sl.SLCodec;
import jade.content.ContentElement;
import jade.content.lang.Codec.CodecException;
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
import meteo.Ontology;
import meteo.onto.*;




/**
 * ***************************************************************
 * JADE - Java Agent DEvelopment Framework is a framework to develop
 * multi-agent systems in compliance with the FIPA specifications.
 * Copyright (C) 2000 CSELT S.p.A.
 * 
 * GNU Lesser General Public License
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation,
 * version 2.1 of the License.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA.
 * **************************************************************
 */


/**
   This example shows how to subscribe to the DF agent in order to be notified 
   each time a given service is published in the yellow pages catalogue.
   In this case in particular we want to be informed whenever a service of type
   "Weather-forecast" for Italy becomes available.
   @author Giovanni Caire - TILAB
 */
public class SubscriberAg extends Agent {

  private SLCodec langage;
  private ACLMessage msg;

protected void setup() {
	langage = new SLCodec();
	getContentManager().registerLanguage(new SLCodec());
	getContentManager().registerOntology(Ontology.getInstance());
	  
	try {
        // Build the description used as template for the search
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription templateSd = new ServiceDescription();
        templateSd.setType("meteo");
        template.addServices(templateSd);

        SearchConstraints sc = new SearchConstraints();
        // We want to receive 10 results at most
        sc.setMaxResults(new Long(10));

        DFAgentDescription[] results = DFService.search(this, template, sc);
        if (results.length > 0) {
            System.out.println("Agent "+getLocalName()+" found the following weather-forecast services:");
            for (int i = 0; i < results.length; ++i) {
                DFAgentDescription dfd = results[i];
                AID provider = dfd.getName();
                // The same agent may provide several services; we are only interested
                // in the weather-forcast one
                Iterator it = dfd.getAllServices();
                while (it.hasNext()) {
                    ServiceDescription sd = (ServiceDescription) it.next();
                    if (sd.getType().equals("meteo")) {
                        System.out.println("- Service \""+sd.getName()+"\" provided by agent "+provider.getName());
	                    
                        msg = new ACLMessage(ACLMessage.SUBSCRIBE);
	                    //AID meteoAg = new AID(provider.getName(), AID.ISLOCALNAME);
	            	
	            		msg.addReceiver(provider);
	            		msg.setLanguage(langage.getName());
	            		msg.setOntology(Ontology.NAME);
	            		//Strong wind
	            		IsStrong content = new IsStrong();
	            		//isStrong.
	            		
	            		//Alert content = new Alert();
	            		//content.setLimit(0);
	            		//content.setInventory(new Inventory(inventoryAg));
	            		//content.setQuantity(10);
	            		//id = contained_product_ids.get(name);
	            		//content.setProduct(new Product(id));
	            		try {
	            			getContentManager().fillContent(msg, content);
	            			//frame.appendLog(msg.toString(), false);
	            			addBehaviour(new SubscriptionInitiator(this, msg) {

	            				@Override
	            				protected void handleInform(ACLMessage msg) {
	            					
	            					//frame.appendLog(msg.toString(), false);
	            					ContentElement reply;
	            					try {
	            						reply = getContentManager().extractContent(msg);
	            					
	            						if (reply instanceof IsStrong) {
	            							IsStrong isStrong = (IsStrong)reply;
	            							//float speed = alert.getWindAlert().getValue();
	            							System.out.print("------------Wind alert : ");System.out.println(isStrong.getWind().getSpeed());
	            							//frame.setQueryResult(q);
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
                }
            }
        }   
        else {
            System.out.println("Agent "+getLocalName()+" did not find any weather-forecast service");
        }
    }
    catch (FIPAException fe) {
        fe.printStackTrace();
    }
} 
}

