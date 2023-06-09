package lob;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// TODO for precision, change prices from double to java.math.BigDecimal

public class OrderBook {
	private List<Trade> tape = new ArrayList<Trade>();
	private OrderTree bids = new OrderTree();
	private OrderTree asks = new OrderTree();
	private double tickSize;
	private int time;
	private int nextQuoteID;
	private int lastOrderSign;
	
	public OrderBook(double tickSize) {
		this.tickSize = tickSize;
		this.reset();
	}
	
	public void reset() {
		tape.clear();
		bids.reset();
		asks.reset();
		time = 0;
		nextQuoteID = 0;
		lastOrderSign=1;
	}
	
	
	/**
	 * Clips price according to tickSize
	 * 
	 * @param price
	 * @return
	 */
	private double clipPrice(double price) {
		int numDecPlaces = (int)Math.log10(1 / this.tickSize);
		BigDecimal bd = new BigDecimal(price);
		BigDecimal rounded = bd.setScale(numDecPlaces, BigDecimal.ROUND_HALF_UP);
		return rounded.doubleValue();
	}
	
	
	public OrderReport processOrder(Order quote, boolean verbose) {

		//System.out.println("New order submitted: \n"+ quote.toString());

		boolean isLimit = quote.isLimit();
		OrderReport oReport;
		// Update time
		this.time = quote.getTimestamp();
		if (quote.getQuantity() <= 0 ) {
			throw new IllegalArgumentException("processOrder() given qty <= 0");
		}
		if (isLimit) {
			double clippedPrice = clipPrice(quote.getPrice());
			quote.setPrice(clippedPrice);
			oReport = processLimitOrder(quote, verbose);
		} else {
			oReport = processMarketOrder(quote, verbose);
		}

		return oReport;
	}
	
	
	private OrderReport processMarketOrder(Order quote, boolean verbose) {
		ArrayList<Trade> trades = new ArrayList<Trade>();
		String side = quote.getSide();
		int qtyRemaining = quote.getQuantity();
		if (side =="bid") {
			this.lastOrderSign = 1;
			while ((qtyRemaining > 0) && (this.asks.getnOrders() > 0)) {
				OrderList ordersAtBest = this.asks.minPriceList();
				qtyRemaining = processOrderList(trades, ordersAtBest, qtyRemaining,
												quote, verbose);
			}
		}else if(side=="offer") {
			this.lastOrderSign = -1;
			while ((qtyRemaining > 0) && (this.bids.getnOrders() > 0)) {
				OrderList ordersAtBest = this.bids.maxPriceList();
				qtyRemaining = processOrderList(trades, ordersAtBest, qtyRemaining,
												quote, verbose);
			}
		}else {
			throw new IllegalArgumentException("order neither market nor limit: " + 
				    						    side);
		}
		OrderReport report = new OrderReport(trades, false);
		return  report;
	}
	
	
	private OrderReport processLimitOrder(Order quote, 
										  boolean verbose) {
		boolean orderAddedToBook = false;
		ArrayList<Trade> trades = new ArrayList<Trade>();
		String side = quote.getSide();
		int qtyRemaining = quote.getQuantity();
		double price = quote.getPrice();
		if (side=="bid") {
			this.lastOrderSign = 1;
			while ((this.asks.getnOrders() > 0) && 
					(qtyRemaining > 0) && 
					(price >= asks.minPrice())) {
				OrderList ordersAtBest = asks.minPriceList();
				qtyRemaining = processOrderList(trades, ordersAtBest, qtyRemaining,
												quote, verbose);
			}
			// If volume remains, add order to book
			if (qtyRemaining > 0) {
				quote.setqId(this.nextQuoteID);
				quote.setQuantity(qtyRemaining);
				this.bids.insertOrder(quote);
				orderAddedToBook = true;
				this.nextQuoteID+=1;
			} else {
				orderAddedToBook = false;
			}
		} else if (side=="offer") {
			this.lastOrderSign = -1;
			while ((this.bids.getnOrders() > 0) && 
					(qtyRemaining > 0) && 
					(price <= bids.maxPrice())) {
				OrderList ordersAtBest = bids.maxPriceList();
				qtyRemaining = processOrderList(trades, ordersAtBest, qtyRemaining,
												quote, verbose);
			}
			// If volume remains, add to book
			if (qtyRemaining > 0) {
				quote.setqId(this.nextQuoteID);
				quote.setQuantity(qtyRemaining);
				this.asks.insertOrder(quote);
				orderAddedToBook = true;
				this.nextQuoteID+=1;
			} else {
				orderAddedToBook = false;
			}
		} else {
			throw new IllegalArgumentException("order neither market nor limit: " + 
				    						    side);
		}
		OrderReport report = new OrderReport(trades, orderAddedToBook);
		if (orderAddedToBook) {
			report.setOrder(quote);
		}
		return report;
	}
	
	
	private int processOrderList(ArrayList<Trade> trades, OrderList orders,
								int qtyRemaining, Order quote,
								boolean verbose) {
		String side = quote.getSide();
		int buyer, seller;
		int takerId = quote.gettId();
		int time = quote.getTimestamp();
		while ((orders.getLength()>0) && (qtyRemaining>0)) {
			int qtyTraded = 0;
			Order headOrder = orders.getHeadOrder();
			if (qtyRemaining < headOrder.getQuantity()) {
				qtyTraded = qtyRemaining;
				if (side=="offer") {
					this.bids.updateOrderQty(headOrder.getQuantity()-qtyRemaining, 
											 headOrder.getqId());
				} else {
					this.asks.updateOrderQty(headOrder.getQuantity()-qtyRemaining, 
											 headOrder.getqId());
				}
				qtyRemaining = 0;
			} else {
				qtyTraded = headOrder.getQuantity();
				if (side=="offer") {
					this.bids.removeOrderByID(headOrder.getqId());
				} else {
					this.asks.removeOrderByID(headOrder.getqId());
				}
				qtyRemaining -= qtyTraded;
			}
			if (side=="offer") {
				buyer = headOrder.gettId();
				seller = takerId;
			} else {
				buyer = takerId;
				seller = headOrder.gettId();
			}
			Trade trade = new Trade(time, headOrder.getPrice(), qtyTraded, 
									headOrder.gettId(),takerId, buyer, seller, 
									headOrder.getqId());
			trades.add(trade);
			this.tape.add(trade);
			if (verbose) {
				System.out.println(trade);
			}
		}
		return qtyRemaining;
	}
	
	
	public void cancelOrder(String side, int qId, int time) {
		this.time = time;
		if (side=="bid") {
			if (bids.orderExists(qId)) {
				bids.removeOrderByID(qId);
			}
		} else if (side=="offer") {
			if (asks.orderExists(qId)) {
				asks.removeOrderByID(qId);
			}
		} else {
			System.out.println("cancelOrder() given neither 'bid' nor 'offer'");
			System.exit(0);
		}
	}
	
	
	public void modifyOrder(int qId, HashMap<String, String> quote) {
		// TODO implement modify order
		// Remember if price is changed must check for clearing.
	}
	
	
	public int getVolumeAtPrice(String side, double price) {
		price = clipPrice(price);
		int vol = 0;
		if(side=="bid") {
			if (bids.priceExists(price)) {
				vol = bids.getPriceList(price).getVolume();
			}
		} else if (side=="offer") {
			if (asks.priceExists(price)) {
				vol = asks.getPriceList(price).getVolume();
			}
		} else {
			System.out.println("modifyOrder() given neither 'bid' nor 'offer'");
			System.exit(0);
		}
		return vol;
		
	}
	
	public double getBestBid() {
		return bids.maxPrice();
	}
	
	public double getWorstBid() {
		return bids.minPrice();
	}
	
	public double getBestOffer() {
		return asks.minPrice();
	}
	
	public double getWorstOffer() {
		return asks.maxPrice();
	}
	
	public int getLastOrderSign() {
		return lastOrderSign;
	}
	
	public int volumeOnSide(String side) {
		if (side=="bid") {
			return this.bids.getVolume();
		} else if (side=="offer") {
			return this.asks.getVolume();
		} else {
			throw new IllegalArgumentException("order neither market nor limit: " + 
				    							side);
		}
	}
	
	public double getTickSize() {
		return tickSize;
	}
	
	public double getSpread() {
		return this.asks.minPrice()-this.bids.maxPrice();
	}
	
	public double getMid() {
		return this.getBestBid()+(this.getSpread()/2.0);
	}
	
	public boolean bidsAndAsksExist() {
		return ((this.bids.nOrders>0)&&(this.asks.nOrders>0));
	}
	
	public String toString() {
		StringWriter fileStr = new StringWriter();
		fileStr.write("Seq: " + this.time + "\n");
		fileStr.write(" ********** The Order Book **********\n");
		fileStr.write("       ------- Bids --------   \n");
		fileStr.write("Price		Qty		Seq		Qid		TxnId\n");
		if (bids.getnOrders() > 0) {
			fileStr.write(bids.toString());
		}
		fileStr.write("\n\n       ------ Offers -------   \n");
		fileStr.write("Price		Qty		Seq		Qid		TxnId\n");
		if (asks.getnOrders() > 0) {
			fileStr.write(asks.toString());
		}
		fileStr.write("\n\n   -------- Trades  ---------   ");
		if (!tape.isEmpty()) {
			for (Trade t : tape) {
				fileStr.write(t.toString());
			}
		}
		fileStr.write("\n --------------------------------\n");
		return fileStr.toString();
	}


	public List<Trade> getTape() {
		return tape;
	}

}
