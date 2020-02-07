package ProductAllocation;

import DataFiles.Order;

public class OrderDelivery {


    Order order;
    int period;
    double volume;

    public OrderDelivery(Order order, int period, double volume){
        this.order = order;
        this.period = period;
        this.volume = volume;
    }
}
