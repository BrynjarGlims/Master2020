package Individual;
import DataFiles.*;


import java.util.Arrays;

public class Individual {

    public GiantTour giantTour;
    public VehicleType vehicleType;

    public Data data;

    public Individual(Data data){
        this.data = data;
    }

    public boolean isFeasible() {
        //NOTE: AddSplit must be called in advance of this method
        if (!hasValidTimeWindows()) {
            return false;
        }
        else if (!hasValidVehicleCapacity()) {
            return false;

        }
        return true;
    }

    public boolean hasValidTimeWindows() {
        //Todo: needs to be implemented
        return true;
    }

    public boolean hasValidVehicleCapacity() {
        //Todo: needs to be implemented
        return true;
    }

    public static void main(String[] args){
        Data data = DataReader.loadData();
        System.out.println(data.customers.length);
        System.out.println(Arrays.toString(data.customers[0].timeWindow[1]));


        Individual individual = new Individual(data);
    }

    public double evaluateIndividual() {
        return;
    }





}

