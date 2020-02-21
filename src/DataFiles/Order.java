package DataFiles;

public class Order {

    // All values can be found in FFV

    public int orderID;
    public int customerID;
    public double volume;
    public boolean isDividable;
    public String commodityFlow;
    public int storeFrequency;
    public int minFrequency;
    public int maxFrequency;
    public double minVolume;
    public double maxVolume;


    public Order(int orderID, int customerID, double volume, boolean isDividable, String commodityFlow, int storeFrequency,
                 int minFrequency, int maxFrequency ){

        this.orderID = orderID;
        this.customerID = customerID;
        this.volume = volume;
        this.isDividable = isDividable;
        this.commodityFlow = commodityFlow;
        this.storeFrequency = storeFrequency;
        this.minFrequency = minFrequency;
        this.maxFrequency = maxFrequency;
        this.setMinVolume();
        this.setMaxVolume();

    }

    private void setMaxVolume(){
        this.minVolume = volume/maxFrequency; // TODO: 21/02/2020  : change into something more reasonable

    }

    private void setMinVolume(){
        this.maxVolume = volume/minFrequency;    // TODO: 21/02/2020  : change into something more reasonable


    }









}
