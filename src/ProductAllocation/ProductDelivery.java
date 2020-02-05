package ProductAllocation;

import DataFiles.Product;

public class ProductDelivery {


    Product product;
    int period;
    double volume;

    public ProductDelivery(Product product, int period, double volume){
        this.product = product;
        this.period = period;
        this.volume = volume;
    }
}
