package ProductAllocation;

import DataFiles.Order;

public class ProductDelivery {


    Order order;
    int period;
    double volume;

    public ProductDelivery(Order order, int period, double volume){
        this.order = order;
        this.period = period;
        this.volume = volume;
    }
}
