package ProductAllocation;

import DataFiles.Order;

public class OrderDelivery { 

    public Order order;
    public double[] orderVolumes;
    public int[] orderPeriods;
    public boolean dividable;
    private int period;

    public OrderDelivery(int numberOfPeriods, Order order){
        this.dividable = order.isDividable;
        this.order = order;
        orderVolumes = new double[numberOfPeriods];
        orderPeriods = new int[numberOfPeriods];
    }


    public OrderDelivery(int numberOfPeriods, Order order, int period, double volume) {
        this(numberOfPeriods, order);
        orderPeriods[period] = 1;
        orderVolumes[period] = volume;
        if (!dividable){
            this.period = period;
        }
    }

    public void addDelivery(int period, double volume){
        orderVolumes[period] = volume;
        orderPeriods[period] = 1;
    }

    public void removeDelivery(int period){
        orderVolumes[period] = 0;
        orderPeriods[period] = 0;
    }

    public int getPeriod(){
        if (dividable){
            return -1;
        }
        else {
            return period;
        }
    }


    public String toString(){
        String out = "";
        out += "Order: " + order.orderID;
        out += "\nDividable: " + dividable;
        double sum = 0;
        for (int i = 0; i < orderPeriods.length ; i++){
            if (orderPeriods[i] == 1){
                out += "\nPeriod: " + i + "  Volume: " + orderVolumes[i];
                sum += orderVolumes[i];
            }
        }
        out += "\n Total volume: " + sum;
        out += "\n Order volume: " + order.volume;
        return out + "\n";
    }



}
