package Master2020.DataFiles;

import scala.xml.PrettyPrinter;

public class Order {

    // All values can be found in FFV

    public int orderID;
    public int customerOrderID;
    public int customerDividableOrderID;
    public int customerNonDividableOrderID;
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
        this.volume = this.adjustVolume(volume);
        this.isDividable = isDividable;
        this.commodityFlow = commodityFlow;
        this.storeFrequency = storeFrequency;
        this.minFrequency = minFrequency;
        this.maxFrequency = maxFrequency;
        this.setMinVolume();
        this.setMaxVolume();

    }


    private double adjustVolume(double volume){
        return Parameters.scalingVolumeValue*volume; //todo: implement rounding to 0.5
    }
    private void setMinVolume(){
        this.minVolume = volume/maxFrequency*Parameters.lowerVolumeFlexibility;
    }

    private void setMaxVolume(){
        this.maxVolume = volume/minFrequency*Parameters.upperVolumeFlexibility;
    }

    public void setCustomerDividableOrderID(int customerDividableOrderID) {
        this.customerDividableOrderID = customerDividableOrderID;
    }

    public void setCustomerNonDividableOrderID(int customerNonDividableOrderID) {
        this.customerNonDividableOrderID = customerNonDividableOrderID;
    }

    public void setCustomerOrderID(int customerOrderID){
        this.customerOrderID = customerOrderID;
    }
}
