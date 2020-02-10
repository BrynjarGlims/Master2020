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
        //TODO: needs to be implemented
        return 0.0;
    }

    public void AdSplit() {
        /*
        Split into trips:

         */





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

