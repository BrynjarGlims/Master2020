package DataFiles;

public class Order {

    // All values can be found in FFV

    public int OrderID;
    public int CustomerID;
    public double volume;
    public boolean isDividable;
    public String commodityFlow;
    public int storeFrequency;
    public int minFrequency;
    public int maxFrequency;

    public Order(int OrderID, int CustomerID, double volume, boolean isDividable, String commodityFlow, int storeFrequency,
                 int minFrequency, int maxFrequency ){
        this.OrderID = OrderID;
        this.CustomerID = CustomerID;
        this.volume = volume;
        this.isDividable = isDividable;
        this.commodityFlow = commodityFlow;
        this.storeFrequency = storeFrequency;
        this.minFrequency = minFrequency;
        this.maxFrequency = maxFrequency;

    }





}
