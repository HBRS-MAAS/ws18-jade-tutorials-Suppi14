package maas.tutorials;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.ShutdownPlatform;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;


@SuppressWarnings("serial")
public class BookBuyerAgent extends Agent{
	//title for the book
	private String titleofbooks; 

	//no of seller agents
	private AID[] sellerAgents = {new AID("seller1", AID.ISLOCALNAME),
			new AID("seller2", AID.ISLOCALNAME),
			new AID("seller3", AID.ISLOCALNAME)};
	private int max_no_of_books = 3;
	protected void setup() {
		// Printout a welcome message
		
		Object[] args = getArguments();
		if(args!=null && args.length>0) {
			
			titleofbooks = (String) args[0];
			System.out.println("Request to buy" +titleofbooks);
			System.out.println("Hello! Buyer-agent "+getAID().getName()+" wants to buy"+titleofbooks);
			addBehaviour(new RequestPerformer(titleofbooks));
			addBehaviour(new TickerBehaviour(this, 60000) {
				protected void onTick() {

					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					sd.setType("book-selling");
					template.addServices(sd);
					try {
						DFAgentDescription[] result = DFService.search(myAgent, template);
						sellerAgents = new AID[result.length];
						for (int i = 0; i < result.length; ++i) {
							sellerAgents[i] = result[i].getName();
						}
					}
					catch (FIPAException fe) {
						fe.printStackTrace();
					}
				}
			} );
		}
		else {
			System.out.println("Terminating agent");
			doDelete();
		}
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			//e.printStackTrace();
		}
		//		addBehaviour(new TickerBehaviour(this,60000) {
		//			protected void onTick() {
		//				myAgent.addBehaviour(new RequestPerformer());
		//			}	
		//		});
		//		addBehaviour(new shutdown());

	}
	protected void takeDown() {
		System.out.println(getAID().getLocalName() + ": Terminating.");
	}

	// Taken from http://www.rickyvanrijn.nl/2017/08/29/how-to-shutdown-jade-agent-platform-programmatically/
	private class shutdown extends OneShotBehaviour{
		public void action() {
			ACLMessage shutdownMessage = new ACLMessage(ACLMessage.REQUEST);
			Codec codec = new SLCodec();
			myAgent.getContentManager().registerLanguage(codec);
			myAgent.getContentManager().registerOntology(JADEManagementOntology.getInstance());
			shutdownMessage.addReceiver(myAgent.getAMS());
			shutdownMessage.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
			shutdownMessage.setOntology(JADEManagementOntology.getInstance().getName());
			try {
				myAgent.getContentManager().fillContent(shutdownMessage,new Action(myAgent.getAID(), new ShutdownPlatform()));
				myAgent.send(shutdownMessage);
			}
			catch (Exception e) {
				//LOGGER.error(e);
			}

		}
	}

	/*
	 * Inner Class RequestPerformer
	 * This is a behaviour used by Book buyer agents
	 */
	private class RequestPerformer extends Behaviour{
		private AID bestSeller;
		private int bestPrice;
		private int repliesCnt=0;
		private MessageTemplate mt;
		private int step=0;
		private String titleofbooks;

		public RequestPerformer(String titleofbooks) {
			this.titleofbooks = titleofbooks;
		}
		public void action() {
			switch(step) {
			case 0:
				//send cfp to all sellers
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				for(int i=0; i< sellerAgents.length; ++i) {
					cfp.addReceiver(sellerAgents[i]);
				}
				cfp.setContent(titleofbooks);
				cfp.setConversationId("book-trade");
				cfp.setReplyWith("cfp"+System.currentTimeMillis()); //Unique value
				myAgent.send(cfp);
				//Prepare the template to get proposals
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade1"),
						MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
				step = 1;
				break;
			case 1:
				//Receive all proposals/refusals from seller agents
				ACLMessage reply = myAgent.receive(mt);
				if(reply !=null) {
					//Reply received
					if(reply.getPerformative() == ACLMessage.PROPOSE) {
						//This is an offer
						int price = Integer.parseInt(reply.getContent());
						if(bestSeller == null || price < bestPrice) {
							bestPrice = price;
							bestSeller = reply.getSender();
						}
					}
					repliesCnt ++;
					if(repliesCnt >= sellerAgents.length) {
						//We received all replies
						step = 2;
					}
				}
				else {
					block();
				}
				break;
			case 2:
				// Send the purchase order to the seller that provided the best offer
				ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
				order.addReceiver(bestSeller);
				order.setContent(titleofbooks);
				order.setConversationId("book-trade");
				order.setReplyWith("order"+System.currentTimeMillis());
				myAgent.send(order);
				// Prepare the template to get the purchase order reply
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
						MessageTemplate.MatchInReplyTo(order.getReplyWith()));
				step = 3;
				break;
			case 3:
				// Receive the purchase order reply
				reply = myAgent.receive(mt);
				if (reply != null) {
					// Purchase order reply received
					if (reply.getPerformative() == ACLMessage.INFORM) {
						// Purchase successful. We can terminate
						System.out.println(titleofbooks+ " successfully purchased.");
						System.out.println("Price = "+bestPrice);
						myAgent.doDelete();
					}
					step = 4;
				}
				else {
					block();
				}
				break;
			}
		}
		public boolean done() {
			if (step == 2 && bestSeller == null) {
				System.out.println("Attempt failed: "+titleofbooks+" not available for sale");
			}
			System.out.println("return back best seller");
			return ((step==2 && bestSeller == null || step == 4 && max_no_of_books ==3));
			
		}
	}
	// End of inner class RequestPerformer
}
