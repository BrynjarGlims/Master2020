package Individual;
import DataFiles.*;
import ProductAllocation.OrderDistribution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Individual {

    public GiantTour[][] giantTour;  //period, vehicleType
    public VehicleType vehicleType;
    public double costOfIndividual;
    public ArrayList<ArrayList<Integer>> tripSplit;
    public double[][] arcCostMatrix;
    public OrderDistribution orderDistribution;

    public Data data;

    public Individual(Data data, OrderDistribution orderDistribution){
        this.data = data;
        this.orderDistribution = orderDistribution;
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

    public double evaluateIndividual() {
        //TODO: needs to be implemented
        return 0.0;
    }


    public void calculateArcCost(int i, int j, double loadSum) {
        //calculate trip cost according to Vidal's formula and update cost matrix
        //TODO 11.2: insert vehicle capacity
        double tempCost = data.distanceMatrix[i][j] + Parameters.initialOvertimePenalty*(Math.max(0, data.distanceMatrix[i][j] - Parameters.maxTripDuration))
                + Parameters.initialCapacityPenalty(Math.max(0, loadSum - CAPACITY ));
        this.arcCostMatrix[i][j] = tempCost;
    }


    public void createTrips(int p, int vt){
        ArrayList<Integer> customerSequence = this.giantTour[p][vt].chromosome;

        // Calculate cost matrix
        for (int i = 0; i < data.customers.length; i++) {
            double loadSum = 0;
            for (int j = i+1; j < data.customers.length; j++) {
                loadSum += orderDistribution.orderDistribution[p][customerSequence.get(j)];
                calculateArcCost(i, j, loadSum);
            }
        }

        //SHORTEST PATH:
        //extract order quantities for customers in the giant tour
        ArrayList<Integer> listOfOrders = new ArrayList<Integer>();
        for (int i = 0; i < customerSequence.size(); i++) {
            listOfOrders.add(customerSequence.get(i));
        }

        //create a LabelList with length = customers in customerSequence

        double[] costLabel = new double[customerSequence.size()];
        int[] predecessorLabel = new int[customerSequence.size()];

        costLabel[0] = 0;

        for (int i = 1; i < customerSequence.size(); i++){
            costLabel[i] = 100000;}

        double tempLoad = 0;
        double tempCost = 0;
        for (int i = 1; i < customerSequence.size(); i++) {
            int j = i;
            while (j <= customerSequence.size() || tempLoad <= CAPACITY || tempCost <= Parameters.maxTripDuration) {
                tempLoad += listOfOrders.get(j);
                if (i == j) {
                    // Vidal also adds a delivery cost to each customer - we do not have that?
                    tempCost = arcCostMatrix[0][j] + arcCostMatrix[j][0];
                } else {
                    tempCost = tempCost - arcCostMatrix[j - 1][0] + arcCostMatrix[j - 1][j] + arcCostMatrix[j][0];
                }
                //check feasibility
                //TODO 11.2: insert vehicle capacity
                if (tempLoad <= CAPACITY && tempCost <= Parameters.maxTripDuration) {
                    if ((costLabel[i - 1] + tempCost) < costLabel[i]) {
                        costLabel[j] = costLabel[i - 1] + tempCost;
                        predecessorLabel[j] = i - 1;
                    }
                    j += 1;
                }
            }
        }

        //extract VRP solution
        double[] listOfTrips = new double[customerSequence.size()];
        for (int i = 1; i < customerSequence.size(); i++) {
            listOfTrips[i] = 0;
        }

        int t = 0;
        int j = customerSequence.size();
        while (int i != 0) {
            t += 1;
            i = predecessorLabel[i];
            for (int k = i+1; k < j; j++) {
                //add node at end of trip (
            }
            j = i;
        }

        }

    public void distributeTrips(){

    }

    /*
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

    public static void main(String[] args){
        Data data = DataReader.loadData();
        System.out.println(data.customers.length);
        System.out.println(Arrays.toString(data.customers[0].timeWindow[1]));

        Individual individual = new Individual(data);
        individual.createTrips();


    }

}





