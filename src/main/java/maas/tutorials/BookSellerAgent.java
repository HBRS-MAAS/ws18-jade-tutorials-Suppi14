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
import java.util.*;

@SuppressWarnings("serial")
public class BookSellerAgent extends Agent{
	// The catalogue of ebook for sale 
	 private Hashtable catalogue_ebooks;
	 // The catalogue of paperback for sale (maps the title of a book to its price)
	    private Hashtable catalogue_Paperbacks;
	    // The availability of paperbacks (maps the title of a paperback to its availability)
	    private Hashtable availablePaperbacks;
	    // Total number of paperbacks
	    private int paperbackcopies = 20;
	    // The list of known buyer agents
	    private AID [] buyerAgents;

	//Agent initializations
	protected void setup() {
//		final_book_list = new ArrayList();
		System.out.println("Hello! Seller Agents"+getAID().getName()+" is ready");
		create_catalogue();

			// Register the book-selling service in the yellow pages
			DFAgentDescription dfd = new DFAgentDescription();
			dfd.setName(getAID());
			ServiceDescription sd = new ServiceDescription();
			sd.setType("book-selling");
			sd.setName("JADE-book-trading");
			dfd.addServices(sd);
			try {
			DFService.register(this, dfd);
			}
			catch (FIPAException fe) {
			fe.printStackTrace();
			}
			

		//Add the behaviour serving requests for offer from buyer agents
		addBehaviour(new OfferRequestsServer());

		//Add the behaviour serving purchase orders from buyer agents
		addBehaviour(new PurchaseOrdersServer());
	
	  try {
          Thread.sleep(3000);
      } catch (InterruptedException e) {
          //e.printStackTrace();
      }
	  addBehaviour(new TickerBehaviour(this, 5000) {
          protected void onTick() {
        	  DFAgentDescription template = new DFAgentDescription();
  			ServiceDescription sd = new ServiceDescription();
  			sd.setType("book-buying");
  			template.addServices(sd);
  			try {
  				DFAgentDescription [] result = DFService.search(myAgent, template);
  				buyerAgents = new AID [result.length];
  				for (int i = 0; i < result.length; ++i) {
  					buyerAgents[i] = result[i].getName();
  				}
  			}
  			catch (FIPAException fe) {
  				fe.printStackTrace();
  			}
              if(buyerAgents.length == 0){
                  System.out.println("There are no buyers terminating");
                  addBehaviour(new shutdown());
              }
          }

      } );

  }
	//Put agent clean up operations here
	protected void takeDown() {
	// Deregister from the yellow pages
		try {
		DFService.deregister(this);
		}
		catch (FIPAException fe) {
		fe.printStackTrace();
		}
		//Printout a dismissal message
		System.out.println("Seller agent"+getAID().getName()+"terminating.");
	}
	
	protected void create_catalogue(){
        List<String> ebooks_list = new Vector<>();
        List<String> paperback_list = new Vector<>();
        // Create the catalogue of ebooks
        catalogue_ebooks = new Hashtable();
     // Create the catalogue of paperbacks
        catalogue_Paperbacks = new Hashtable();      
        // Create the inventory of paperbacks
        availablePaperbacks = new Hashtable();
        String[] list_of_books = {"Murder on the Orient Express","Kite Runner","The Alchemist","If tomorrow comes"};
        int[] prices = {1000,200,4000,8000};
        //only for 2 books
        int[] list_paperback_copies = {10,10};
        Random rand = new Random();
        for(int i=0; i<2;i++) {
        	ebooks_list.add(list_of_books[i]);
		}
       
        for(int i=2; i<4;i++) {
        	paperback_list.add(list_of_books[i]);
		}     
        for(int i=0;i<ebooks_list.size();i++) {
        	catalogue_ebooks.put(ebooks_list.get(i), prices[rand.nextInt(4)]);
        }
        System.out.println("catalogue_ebooks"+catalogue_ebooks);
        for (int i=0; i<paperback_list.size(); i++){
            catalogue_Paperbacks.put(paperback_list.get(i),prices[rand.nextInt(4)]);
            availablePaperbacks.put(paperback_list.get(i), list_paperback_copies[rand.nextInt(2)]);
		}
    }

	private class OfferRequestsServer extends CyclicBehaviour{
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive();
//			System.out.println("In offerRequest"+msg);
			if(msg!= null) {
				//Message received. Process it
				String title = msg.getContent();
				
				ACLMessage reply = msg.createReply();
				Integer price = (Integer) 0;
				Integer quantity = (Integer)0;
                // Check if title exists in catalogue_ebooks and catalogue_Paperbacks
                boolean isebook = catalogue_ebooks.containsKey(title);
                boolean ispaperback = catalogue_Paperbacks.containsKey(title);
                if(isebook) {
                	price = (Integer) catalogue_ebooks.get(title); 
                	reply.setPerformative(ACLMessage.PROPOSE);
 				   	reply.setContent(String.valueOf(price.intValue()));
                }
                else if(ispaperback){
                   price = (Integer) catalogue_Paperbacks.get(title);
                   quantity = (Integer) availablePaperbacks.get(title);
                if (quantity >0){
                   reply.setPerformative(ACLMessage.PROPOSE);
				   reply.setContent(String.valueOf(price.intValue()));
                }
			}
				else {
					// The requested book is NOT available for sale.
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

                // Check if title exists in catalogue_ebooks and catalogue_Paperbacks
                boolean isebook = catalogue_ebooks.containsKey(title);
                boolean ispaperback = catalogue_Paperbacks.containsKey(title);

                if (ispaperback){
                    Integer quantity = (Integer) availablePaperbacks.get(title);

                    if (quantity > 0){
                        // Sell the book
                        reply.setPerformative(ACLMessage.INFORM);
    					System.out.println("****"+title+" is sold to agent "+msg.getSender().getName()+"*****");
                        // Decrease the quantity in inventory
                        quantity = (Integer) availablePaperbacks.get(title);
                        availablePaperbacks.put(title, quantity-1);
                    }
                    else{
                        // The requested book has been sold to another buyer in the meanwhile .
    					reply.setPerformative(ACLMessage.FAILURE);
    					reply.setContent("not-available");
                    }
                }
                else if (isebook){ 
                    reply.setPerformative(ACLMessage.INFORM);
                    System.out.println("****"+title+" sold to agent "+msg.getSender().getName()+"****");
                }

				myAgent.send(reply);
			}
			else {
				block();
			}
		}
		}  // End of inner class OfferRequestsServer

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