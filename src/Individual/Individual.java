package Individual;
import DataFiles.*;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Individual {

    public GiantTour giantTour;
    public VehicleType vehicleType;
    public double costOfIndividual;
    public ArrayList<ArrayList<Integer>> tripSplit;

    public Data data;

    public Individual(Data data){
        this.data = data;
        this.costOfIndividual = costOfIndividual;
    }

    public boolean isFeasible() {
        //NOTE: AdSplit must be called in advance of this method
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
        //TODO: needs to be implemented
        return 0.0;
    }

    public void cost_of_period() {
        //demand =

    }

    public void createTrips(){
        giantTour.chromosome
        //customer indices
        ArrayList<ArrayList<Integer>> tripSplit = ;
        return tripSplit;

    }

    public void distributeTrips(){

    }

    //solves for each period
    public void AdSplit() {
        for (int p = 0; p < Parameters.numberOfPeriods; p++) {
            for (VehicleType vt : this.data.vehicleTypes) {
                createTrips(p, vt);
                distributeTrips(p, vt);
            }
        }
        /*
        Split into trips by computing shortest path:

        For each (customer, period): copy order demand into a list
        create a new list storing demand

        arcCost =

         */


        //update cost: this.costOfIndividual


        //arcCost = driving time + overtime*punishment + overload*punishment
        /*
        Assign trips to vehicles:

        1: Compute shortest path on graph H

        LABELING ALGORITHM:
        2: For all customers:
           Initialize LabelList[i]=empty
        3: current = 0
        4: while current < n:
            succ = get_succ(current)
            load = get_load(current)
            time = get_best_in_time(current)
            for all labels in L:
                for all k=1 --> m: (k represents an index in the label)
                    update all label fields for each node (1-->m)
                    sort fields based on driving time
                    L_cost update
                    L_predecessor = current
                    if !label_dominated():
                        List_of_labels_to_expand: add L*
                        List_of_labels_to_expand: remove dominated labels
            current = succ




         */
    }





}

