package maas.tutorials;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;


import java.util.*;

@SuppressWarnings("serial")
public class BookSellerAgent extends Agent{
	private Hashtable<String, Integer> catalogue;
	ArrayList<Hashtable<String, String>> final_book_list = new ArrayList<Hashtable<String, String>>();

	//Agent initializations
	protected void setup() {
		final_book_list = new ArrayList<Hashtable<String, String>>();
		System.out.println("Hello! Seller Agents"+getAID().getName()+" is ready");
		Object[] args = getArguments();
		if(args!=null && args.length>0) {
			for(int i=0;i<args.length; i++) {
				String book_information = (String)args[i];
				String[] book_info_list = book_information.split("_");
				Hashtable<String, String> info_books = new Hashtable<String, String>();
				info_books.put("book_title", book_info_list[0]);
				info_books.put("quantity",book_info_list[1]);
				info_books.put("price", book_info_list[2]);
				info_books.put("book_type", book_info_list[3]);
				final_book_list.add(info_books);
			}
			System.out.println("print book info"+final_book_list);
		}


		//Add the behaviour serving requests for offer from buyer agents
		addBehaviour(new OfferRequestsServer());

		//Add the behaviour serving purchase orders from buyer agents
		addBehaviour(new PurchaseOrdersServer());
	}
	
	//Put agent clean up operations here
	protected void takeDown() {

		//Printout a dismissal message
		System.out.println("Seller agent"+getAID().getName()+"terminating.");
	}
	/*
	 * This is invoked by the GUI when the user adds a new book for sale
	 */
	public void updateCatalogue(final String title,final int price) {
		addBehaviour(new OneShotBehaviour() {
			public void action() {
				catalogue.put(title, new Integer(price));
			}
		});
	}

	private class OfferRequestsServer extends CyclicBehaviour{
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive();
			if(msg!= null) {
				//Message received. Process it
				String title = msg.getContent();
				ACLMessage reply = msg.createReply();

				Integer price = (Integer) catalogue.get(title);
				if(price != null) {
					//The requested book is available for sale. Reply with the price
					reply.setPerformative(ACLMessage.PROPOSE);
					reply.setContent(String.valueOf(price.intValue()));
				}
				else {
					//The requested book is NOT available for sale.
					reply.setPerformative(ACLMessage.REFUSE);
					reply.setContent("not-available");
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}
	//End of inner class OfferRequestsServer

	/**
		   Inner class PurchaseOrdersServer.
		   This is the behaviour used by Book-seller agents to serve incoming 
		   offer acceptances (i.e. purchase orders) from buyer agents.
		   The seller agent removes the purchased book from its catalogue 
		   and replies with an INFORM message to notify the buyer that the
		   purchase has been sucesfully completed.
	 */
	private class PurchaseOrdersServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// ACCEPT_PROPOSAL Message received. Process it
				String title = msg.getContent();
				ACLMessage reply = msg.createReply();

				Integer price = (Integer) catalogue.remove(title);
				if (price != null) {
					reply.setPerformative(ACLMessage.INFORM);
					System.out.println(title+" sold to agent "+msg.getSender().getName());
				}
				else {
					// The requested book has been sold to another buyer in the meanwhile .
					reply.setPerformative(ACLMessage.FAILURE);
					reply.setContent("not-available");
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
		// End of inner class PurchaseOrderServer
	}
}