package Individual;
import DataFiles.*;

import java.util.Arrays;

public class Individual {

    public GiantTour giantTour;
    public VehicleType vehicleType;
    public DividableProducts dividableProducts;
    public NonDividableProducts nonDividableProducts;
    public Data data;

    public Individual(Data data){
        this.data = data;
    }


    public static void main(String[] args){
        Data data = DataReader.loadData();
        System.out.println(data.customers.length);
        System.out.println(Arrays.toString(data.customers[0].timeWindow[1]));


        Individual individual = new Individual(data);
    }

}
