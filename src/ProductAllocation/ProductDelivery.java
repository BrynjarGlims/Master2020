package ProductAllocation;

import DataFiles.Order;

public class ProductDelivery {


    Order product;
    int period;
    double volume;

    public ProductDelivery(Order product, int period, double volume){
        this.product = product;
        this.period = period;
        this.volume = volume;
    }
}
