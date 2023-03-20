package lob;

import java.util.ArrayList;

public class OrderReport {
	/*
	 * Return after an order is submitted to the lob. Contains:
	 * 	- trades:
	 * 
	 * 	- orderInBook
	 */
	private ArrayList<Trade> trades = new ArrayList<Trade>();
	private boolean orderAddedToBook = false;
	private Order order;
	
	public OrderReport(ArrayList<Trade> trades, 
					   boolean orderInBook) {
		this.trades = trades;
		this.orderAddedToBook = orderInBook;
	}

	public Order getOrder() {
		return order;
	}
	
	public void setOrder(Order order) {
		this.order = order;
	}

	public ArrayList<Trade> getTrades() {
		return trades;
	}

	public boolean isOrderAddedToBook() {
		return orderAddedToBook;
	}
	
	public String toString() {
		String retString = "--- Order Report ---:\nTrades:\n";
		for (Trade t : trades) {
			retString += ("\n" + t.toString());
		}
		if (orderAddedToBook) {
			retString += ("Order added to book: " + order.getSide());
			retString+= ("\nOrders:\n");
			retString += (order.toString());
		}

		return  retString + "\n--------------------------";
	}
	
}
