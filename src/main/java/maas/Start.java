package maas;
import java.util.*;
import java.util.List;
import java.util.Vector;



public class Start {

	private String [] list_of_books = {"Book1","Book2","Book3","Book4","Book5","Book6","Book7","Book8"};
	private int[] prices = {1000,2000,3000,2500};
	private int[] paperbackcopies = {10,10};
	private int[] ebookcopies = {1000,2000,3000,4000};
	private boolean ispaperback = true;
	private int no_of_SellerAgents = 3;
	private int no_of_books_total = 8;
	private int type_book = 2;
	private int no_of_BuyerAgents = 20;
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
		for(int i=0;i<object1.getNo_of_SellerAgents();i++) {
//			agents.add("Seller"+i+":maas.tutorials.BookSellerAgent"+"("+object1.list_of_books[rand.nextInt(4)]+"_10_70"+","+object1.list_of_books[rand.nextInt(4)]+"_20_70"+")");
			agents.add("Seller"+i+":maas.tutorials.BookSellerAgent"+"("+object1.list_of_books[rand.nextInt(6)]+"_"+object1.paperbackcopies[rand.nextInt(2)]+"_"+object1.prices[rand.nextInt(4)]+"_true"+","
																+object1.list_of_books[rand.nextInt(6)]+"_"+object1.paperbackcopies[rand.nextInt(2)]+"_"+object1.prices[rand.nextInt(4)]+"_true"+","
																+object1.list_of_books[rand.nextInt(8)]+"_"+object1.ebookcopies[rand.nextInt(4)]+"_"+object1.prices[rand.nextInt(4)]+"_false"+","
																+object1.list_of_books[rand.nextInt(8)]+"_"+object1.ebookcopies[rand.nextInt(2)]+"_"+object1.prices[rand.nextInt(4)]+"_false"+")");
		
	}
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
