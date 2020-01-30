public class Product {

    // All values can be found in FFV

    public int ProductID;
    public double volume;
    public boolean isDividable;
    public String commodityFlow;
    public int storeFrequency;
    public int minFrequency;
    public int maxFrequency;

    public Product(int ProductID, double volume, boolean isDividable, String commodityFlow, int storeFrequency,
                   int minFrequency, int maxFrequency ){
        this.ProductID = ProductID;
        this.volume = volume;
        this.isDividable = isDividable;
        this.commodityFlow = commodityFlow;
        this.storeFrequency = storeFrequency;
        this.minFrequency = minFrequency;
        this.maxFrequency = maxFrequency;

    }





}
