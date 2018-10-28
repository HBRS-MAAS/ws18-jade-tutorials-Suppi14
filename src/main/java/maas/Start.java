package maas;
import java.util.*;
import java.util.List;
import java.util.Vector;



public class Start {

	private String [] list_of_books = {"Book1","Book2","Book3","Book4","Book5","Book6","Book7","Book8"};
	private int no_of_SellerAgents = 3;
	private int no_of_books_total = 8;
	private int type_book = 2;
	private int no_of_BuyerAgents = 2;
	public String[] getList_of_books() {
		return list_of_books;
	}

	public void setList_of_books(String[] list_of_books) {
		this.list_of_books = list_of_books;
	}

	public int getNo_of_SellerAgents() {
		return no_of_SellerAgents;
	}

	public void setNo_of_SellerAgents(int no_of_SellerAgents) {
		this.no_of_SellerAgents = no_of_SellerAgents;
	}

	public int getNo_of_BuyerAgents() {
		return no_of_BuyerAgents;
	}

	public void setNo_of_BuyerAgents(int no_of_BuyerAgents) {
		this.no_of_BuyerAgents = no_of_BuyerAgents;
	}

	public int getNo_of_books_total() {
		return no_of_books_total;
	}

	public void setNo_of_books_total(int no_of_books_total) {
		this.no_of_books_total = no_of_books_total;
	}

	public int getType_book() {
		return type_book;
	}

	public void setType_book(int type_book) {
		this.type_book = type_book;
	}

	public static void main(String[] args) {
		Random rand = new Random();

		Start object1 = new Start();

		List<String> agents = new Vector<>();
		
		//Adding buyer agents
		for(int i=0; i < object1.getNo_of_BuyerAgents(); i++) {
			agents.add("Buyer"+i+":maas.tutorials.BookBuyerAgent"+"("+object1.list_of_books[rand.nextInt(4)]+","+object1.list_of_books[rand.nextInt(4)]+","+object1.list_of_books[rand.nextInt(4)]+ ")");

		}
		//Adding Seller Agents
			agents.add("Seller1:maas.tutorials.BookSellerAgent(Book1_10_70_paperback,Book5_10_80_paperback,Book7_1000_70_ebook,Book2_1000_80_ebook)");
			agents.add("Seller2:maas.tutorials.BookSellerAgent(Book2_10_70_paperback,Book3_10_80_aperback,Book6_1000_70_ebook,Book8_1000_80_ebbok)");
			agents.add("Seller3:maas.tutorials.BookSellerAgent(Book4_10_70_paperback,Book5_10_80_paperback,Book1_1000_60_ebook,Book8_1500_60_ebook)");
		

		List<String> cmd = new Vector<>();
		cmd.add("-agents");
		StringBuilder sb = new StringBuilder();
		for (String a : agents) {
			sb.append(a);
			sb.append(";");
		}
		cmd.add(sb.toString());
		jade.Boot.main(cmd.toArray(new String[cmd.size()]));
	}



}
