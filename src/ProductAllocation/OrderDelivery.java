package ProductAllocation;

import DataFiles.Order;

public class OrderDelivery { 

    private Order order;
    public double[] orderVolumes;
    public int[] orderPeriods;
    public boolean dividable;


    public OrderDelivery(int numberOfPeriods, Order order, int period, double volume, boolean dividable) {
        this.dividable = dividable;
        this.order = order;
        orderVolumes = new double[numberOfPeriods];
        orderPeriods = new int[numberOfPeriods];
        orderPeriods[period] = 1;
        orderVolumes[period] = volume;
    }

    public void addDelivery(int period, double volume){
        orderVolumes[period] = volume;
        orderPeriods[period] = 1;
    }

    public void removeDelivery(int period){
        orderVolumes[period] = 0;
        orderPeriods[period] = 0;
    }


    public String toString(){
        String out = "";
        out += "Order: " + order.orderID;
        out += "\nDividable: " + dividable;
        for (int i = 0; i < orderPeriods.length ; i++){
            if (orderPeriods[i] == 1){
                out += "\nPeriod: " + i + "  Volume: " + orderVolumes[i];
            }
        }
        return out;
    }



}
