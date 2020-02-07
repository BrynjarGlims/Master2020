package ProductAllocation;

import DataFiles.Order;

public class OrderDelivery { // TODO: 07.02.2020 not overriding split deliveries for the same, but make a list for all order splits 


    Order order;
    int period;
    double volume;

    public OrderDelivery(Order order, int period, double volume){
        this.order = order;
        this.period = period;
        this.volume = volume;
    }
}
