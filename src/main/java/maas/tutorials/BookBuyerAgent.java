//Code based on the examples.bookTrading package in http://jade.tilab.com/download/jade/

package maas.tutorials;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.domain.FIPANames;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.ShutdownPlatform;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.util.List;
import java.util.Vector;
import java.util.Random;


@SuppressWarnings("serial")
public class BookBuyerAgent extends Agent {
	private List<String> catalogue;
	//list of books
	private String[] list_of_books = {"Murder on the Orient Express","Kite Runner","The Alchemist","If tomorrow comes"};
	private int max_no_of_books_allowed = 3;
	private List<String> books_purchased;
	private AID [] sellerAgents;
	private List<String> booktitles = new Vector<>();


	protected void setup() {
		// Welcome message
		System.out.println(getAID().getLocalName()+" is ready.");

//		create_catalogue();
		catalogue = new Vector<>();

		
		for(int i=0; i<list_of_books.length;i++) {
			catalogue.add(list_of_books[i]);
		}
//		System.out.println("printing catalogue"+catalogue);
		
//		Book_titles();
		System.out.println("catalogue size"+catalogue.size());
		Random rand = new Random();
		
		while(booktitles.size()< max_no_of_books_allowed){
			int randomize = rand.nextInt(catalogue.size());
			boolean isTargetbook = booktitles.contains(catalogue.get(randomize));
			if (!isTargetbook)
				booktitles.add(catalogue.get(randomize));
		
		}
//		System.out.println("Booktitle i have********"+booktitles);
		
		books_purchased = new Vector<>();

		System.out.println(getAID().getName()+ " wants to buy  "+booktitles);

		// Register the book-buying service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("book-buying");
		sd.setName("JADE-book-trading");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}

		// Add a TickerBehaviour for each targetBook
		for (String book_title : booktitles) {
			addBehaviour(new TickerBehaviour(this, 4000) {
				protected void onTick() {
					System.out.println("#####"+getAID().getLocalName()+" is trying to buy "+book_title+"######");

					if(books_purchased.contains(book_title)){
						System.out.println("#####"+getAID().getLocalName()+"  has already purchased  " + book_title+"#####");
						System.out.println("Agent"+ getAID().getLocalName()+" bought:");
						System.out.println(books_purchased);
						// Check the number of books bought so far
						if(books_purchased.size() == max_no_of_books_allowed){
							System.out.println("#####"+getAID().getLocalName()+" has purchased " + books_purchased.size() + " books"+"#####");
							// Stop this agent
							doDelete();
						}
						//						else{
						//							System.out.println(getAID().getLocalName()+" has not bought " + max_no_of_books_allowed + " yet");
						//						}

						// Stop the TickerBehaviour that is trying to buy targetBook
						stop();
					}
					else{

						// Update seller agents
						DFAgentDescription template = new DFAgentDescription();
						ServiceDescription sd = new ServiceDescription();
						sd.setType("book-selling");
						template.addServices(sd);
						try {
							DFAgentDescription [] result = DFService.search(myAgent, template);
//							System.out.println("Found the following seller agents:");
							sellerAgents = new AID [result.length];
							for (int i = 0; i < result.length; ++i) {
								sellerAgents[i] = result[i].getName();
//								System.out.println(sellerAgents[i].getName());
							}
						}
						catch (FIPAException fe) {
							fe.printStackTrace();
						}
						// Perform the request
						myAgent.addBehaviour(new RequestPerformer(book_title));

					}
				}

			} );

		}



		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			//e.printStackTrace();
		}
	}


	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		System.out.println(getAID().getLocalName() + ": Terminating.");
	}
//	public void create_catalogue(){
////		
//		catalogue = new Vector<>();
//		catalogue.add("Murder on the Orient Express");
//		catalogue.add("Kite Runner");
//		catalogue.add("The Alchemist");
//		catalogue.add("If tomorrow comes");
//	}
//	
	/**
	   Inner class RequestPerformer.
	   This is the behaviour used by Book-buyer agents to request seller
	   agents the target book.
	 */
	private class RequestPerformer extends Behaviour {
		private AID bestSeller; // The agent who provides the best offer
		private int bestPrice;  // The best offered price
		private int repliesCnt = 0; // The counter of replies from seller agents
		private MessageTemplate mt; // The template to receive replies
		private int step = 0;
		private String book_title;

		public RequestPerformer(String book_title){
			this.book_title = book_title;
		}

		public void action() {
			switch (step) {
			case 0:
				// Send the cfp to all sellers
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				for (int i = 0; i < sellerAgents.length; ++i) {
					cfp.addReceiver(sellerAgents[i]);
				}
				cfp.setContent(book_title);
				
				cfp.setConversationId("book-trade");
				cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
				myAgent.send(cfp);
				// Prepare the template to get proposals
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
						MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
				step = 1;
				break;
			case 1:
				// Receive all proposals/refusals from seller agents
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					// Reply received
					if (reply.getPerformative() == ACLMessage.PROPOSE) {
						// This is an offer
						int price = Integer.parseInt(reply.getContent());
						if (bestSeller == null || price < bestPrice) {
							// This is the best offer at present
							bestPrice = price;
							bestSeller = reply.getSender();
						}
					}
					repliesCnt++;
					if (repliesCnt >= sellerAgents.length) {
						// We received all replies
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
				order.setContent(book_title);
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
						System.out.println("Agent "+getAID().getLocalName()+ " successfully purchased "+ book_title+ " from agent "+reply.getSender().getLocalName());
						System.out.println("Bought at price = "+bestPrice);
						books_purchased.add(book_title);
//						myAgent.doDelete();
					}
					else {
						System.out.println("Attempt failed: requested book has already been sold.");
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
				System.out.println("Attempt failed: "+book_title+" not available for sale");
			}
			return ((step == 2 && bestSeller == null) || step == 4);
		}
	}  // End of inner class RequestPerformer

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
}
