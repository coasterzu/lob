package lob;

import java.util.concurrent.ThreadLocalRandom;

public class App {
    public static void main(String[] args) {
        long s = System.currentTimeMillis();
        double tickSize = 1.0;
        int i = 0;
        OrderBook lob = new OrderBook(tickSize);
        int time = 1;
        int tid= 15000;
        while (i++ < 10000000) {

            int randomNum = ThreadLocalRandom.current().nextInt(100, 500);
            int randomNumQty = ThreadLocalRandom.current().nextInt(1, 1000);
            int randomNumSide = ThreadLocalRandom.current().nextInt(1, 3);
            String sideString;
            if (randomNumSide == 1) {
                sideString = "bid";
            }
            else {
                sideString = "offer";
            }


             lob.processOrder(new Order(time++, true, randomNumQty, tid++, sideString, (double) randomNum), false);



        }
        //System.out.println(lob);
        long e = System.currentTimeMillis();
       // System.out.println(lob);
        System.out.println(e-s);

    }
}
