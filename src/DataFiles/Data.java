package DataFiles;

import javax.swing.*;

public class Data {

    public Customer[] customers;
    public Vehicle[] vehicles;

    public int numPeriods;
    public int numDeliveries;

    public double totalVolume;
    public double targetVolume;

    public Data(Customer[] customers, Vehicle[] vehicles){
        this.customers = customers;
        this.vehicles = vehicles;
        initialize();
    }

    private void initialize(){
        setNumPeriods();
        setTargetVolume();
    }

    private void setNumPeriods(){
        numPeriods = customers[0].timeWindow.length;
    }

    private void setTargetVolume(){
        double totalVolume = 0;
        int numDeliveries = 0;
        for (Customer c : customers){
            for (Product p : c.products){
                totalVolume += p.volume;
                numDeliveries ++;
            }
        }
        this.numDeliveries = numDeliveries;
        this.totalVolume = totalVolume;
        targetVolume = totalVolume/numPeriods;

    }


    public static void main(String[] args){
        Data data = DataReader.loadData();
        for (Customer c : data.customers){
            if (c.timeWindow.length != 6){
                System.out.println(c.customerID);
            }
        }

    }
}
