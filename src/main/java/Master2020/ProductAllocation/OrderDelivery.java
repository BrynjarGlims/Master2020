package Master2020.ProductAllocation;

import Master2020.DataFiles.Order;

public class OrderDelivery implements Cloneable{

    public Order order;
    public double[] orderVolumes;
    public int[] orderPeriods;
    public boolean dividable;
    public int period;

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

    public OrderDelivery clone() throws CloneNotSupportedException {
        OrderDelivery newOrderDelivery = (OrderDelivery) super.clone();
//        OrderDelivery newOrderDelivery = new OrderDelivery(orderPeriods.length, order);
        newOrderDelivery.period = this.period;
        newOrderDelivery.orderVolumes = this.orderVolumes.clone();
        newOrderDelivery.orderPeriods = this.orderPeriods.clone();
        return newOrderDelivery;
    }

    public void addDelivery(int period, double volume){
        orderVolumes[period] = volume;
        orderPeriods[period] = 1;
        if (!dividable) {
            this.period = period;
        }
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
