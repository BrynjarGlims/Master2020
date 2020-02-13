package Individual;
import DataFiles.*;
import ProductAllocation.OrderDistribution;
import org.w3c.dom.ls.LSOutput;
import scala.xml.PrettyPrinter;

import java.awt.color.ICC_Profile;
import java.util.ArrayList;

public class Individual {

    public GiantTour[][] giantTours ;  //period, vehicleType
    public VehicleType vehicleType;
    public double costOfIndividual;
    public ArrayList<ArrayList<Integer>> tripSplit;
    public double[][] arcCostMatrix;
    public OrderDistribution orderDistribution;

    public Data data;
    public int[][] arcCost;  // (i,j) i = from, j = to

    public Individual(Data data, OrderDistribution orderDistribution) {
        this.data = data;
        this.orderDistribution = orderDistribution;
        this.costOfIndividual = costOfIndividual;
        this.giantTours = new GiantTour[data.numberOfPeriods][data.numberOfVehicleTypes];
        for (int p = 0; p < data.numberOfPeriods; p++){
            for (int vt = 0; vt < data.numberOfVehicleTypes; vt++){
                giantTours[p][vt] = new GiantTour();
            }
        }
        this.arcCostMatrix = new double[Parameters.numberOfCustomers][Parameters.numberOfCustomers];

    }

    public boolean isFeasible() {
        //NOTE: AdSplit must be called in advance of this method
        if (!hasValidTimeWindows()) {
            return false;
        } else if (!hasValidVehicleCapacity()) {
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

    public void calculateArcCost(int i, int j, double loadSum, int vehicleTypeIndex) {
        //calculate trip cost according to Vidal's formula and update cost matrix
        double tempCost = data.distanceMatrix[i][j] + Parameters.initialOvertimePenalty*(Math.max(0, data.distanceMatrix[i][j] - Parameters.maxJourneyDuration))
                + Parameters.initialCapacityPenalty*(Math.max(0,loadSum - data.vehicleTypes[vehicleTypeIndex-1].capacity));
        arcCostMatrix[i][j] = tempCost;
        //System.out.println("ArcCost between " +i+","+j +" is updated to " + arcCostMatrix[i][j]);
    }


    public void createTrips(int p, int vt) {
        ArrayList<Integer> customerSequence = this.giantTours[p - 1][vt - 1].chromosome;
        // Calculate cost matrix
        for (int i = 0; i < customerSequence.size(); i++) {
            double loadSum = 0;
            for (int j = i + 1; j < customerSequence.size(); j++) {
                //System.out.println("Order quantity for the customer in the chromosome: "+orderDistribution.orderDistribution[p-1][customerSequence.get(j)]);
                loadSum += orderDistribution.orderDistribution[p - 1][customerSequence.get(j)];
                calculateArcCost(i, j, loadSum, vt);
            }

        }
        //SHORTEST PATH:
        //Create and initialize labels for each node in the auxiliary graph (nodes represent customers)
        double[] costLabel = new double[customerSequence.size()];
        int[] predecessorLabel = new int[customerSequence.size()];

        costLabel[0] = 0;
        for (int i = 1; i < customerSequence.size(); i++) {
            costLabel[i] = 100000;
        }


        //Bellman-Ford

        for (int i = 1; i < customerSequence.size(); i++) {
            int j = i;
            double tempLoadOnTrip = 0;
            double tempCost = 0;
            System.out.println("i: " + i);
            while ((j < customerSequence.size()) && (tempLoadOnTrip <= data.vehicleTypes[vt - 1].capacity) && (tempCost <= Parameters.maxJourneyDuration)) {
                System.out.println("j: " + j);
                //System.out.println("order for customer j: " + orderDistribution.orderDistribution[p-1][j-1]);
                tempLoadOnTrip += orderDistribution.orderDistribution[p - 1][j - 1];
                if (i == j) {
                    // Vidal also adds a delivery cost to each customer - we do not have that?
                    tempCost = arcCostMatrix[0][j] + arcCostMatrix[j][0];
                    //System.out.println("tempcost updated (i=j): " + tempCost);
                    System.out.println("new tempcost: " + tempCost);
                } else {
                    tempCost = tempCost - arcCostMatrix[j - 1][0] + arcCostMatrix[j - 1][j] + arcCostMatrix[j][0];
                    //System.out.println("tempcost updated (i!=j): " + tempCost);
                    System.out.println("new tempcost: " + tempCost);
                }
                //check feasibility
                //TODO 13.2:
                if (tempLoadOnTrip <= data.vehicleTypes[vt - 1].capacity && tempCost <= Parameters.maxJourneyDuration) {
                    if ((costLabel[i-1] + tempCost) < costLabel[j]) {
                        costLabel[j] = costLabel[i - 1] + tempCost;
                        System.out.println("cost label updated for node " + j + "from" + costLabel[j] + "to " + (costLabel[i - 1] + tempCost));
                        predecessorLabel[j] = i - 1;
                        System.out.println("predecessor of " + j + " updated from " + predecessorLabel[j] + "to " + (i-1));
                    }
                }
                j += 1;

            }
        }
        System.out.println("Print predecessor labels: ");
        for (int i = 0; i < predecessorLabel.length; i++) {
            System.out.println(i + ": " + predecessorLabel[i]);
        }

        System.out.println("Print cost labels: ");
        for (int i = 0; i < costLabel.length; i++) {
            System.out.println(i + ": " + costLabel[i]);
        }
    System.out.println("VRP solution: ");
    System.out.println(extractVrpSolution(customerSequence, predecessorLabel));
    extractVrpSolution(customerSequence, predecessorLabel);
    }


    public ArrayList<ArrayList<Integer>> extractVrpSolution(ArrayList<Integer> customerSequence,  int[] predecessorLabel){
        //extract VRP solution by backtracking the shortest path label
        ArrayList<ArrayList<Integer>> listOfTrips = new ArrayList<ArrayList<Integer>>();
        /*
        int t = 0;
        int j = customerSequence.size();
        int i = 10;
        while (j != 0) {
            t += 1;
            i = predecessorLabel[j-1];
            System.out.println("predecessor of customer " + j + ": " + i);
            for (int k = i+1; k < j; j++) {
                listOfTrips[t] = k;
            }
            j = i;
        }
         */
        ArrayList<Integer> tempListOfTrips = new ArrayList<Integer>();
        ArrayList<Integer> tempListOfTripsClear = new ArrayList<Integer>();

        for (int k = 0; k < customerSequence.size(); k++) {
            if (!tempListOfTrips.contains(0)){
                tempListOfTrips.add(0);
            }
            if (predecessorLabel[k] > 0) {
                tempListOfTrips.add(customerSequence.get(k));
            }
            else if (predecessorLabel[k] == 0){
                tempListOfTrips.add(customerSequence.get(k));
                tempListOfTrips.add(0);
                //System.out.println("tempList added to listOfTrips: " + tempListOfTrips);
                listOfTrips.add(tempListOfTrips);
                //TODO 14.2: remove link between lists, such that the adde list is not cleared...
                tempListOfTrips.clear();
                //System.out.println("cleared, tempList = "+ tempListOfTrips);
            }
        }
        return listOfTrips;
    }

    /*
    public void distributeTrips(int p, int vt, ArrayList<ArrayList<Integer>> tripSplit){

        // take this as input,remove afterwards
        ArrayList<Label> currentLabels = new ArrayList<Label>();
        boolean ifFirstElement = true;
        for (ArrayList<Integer> trip : tripSplit ){
            if (ifFirstElement){
                System.out.println("temp");
                //currentLabels.add(new Label(vehicleIndex)); //todo implement

            }

        }

    }

    /*
    //solves for each period
    public void AdSplit() {
        for (int p = 0; p < Parameters.numberOfPeriods; p++) {
            for (int vt = 0; vt < this.data.vehicleTypes.length; vt++) {
                createTrips(p, vt);
                distributeTrips(p, vt, new ArrayList<ArrayList<Integer>>());
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
        OrderDistribution od = new OrderDistribution(data);
        od.makeDistribution();
        Individual individual = new Individual(data, od);
        individual.createTrips(data.numberOfPeriods, data.numberOfVehicleTypes);
        // Print the orderDistribution
        /*
        for (int i = 0; i < od.orderDistribution.length; i++) {
            for (int j = 0; j < od.orderDistribution[0].length; j++) {
                System.out.println("Period: " + i + ", customer: " + j + ", Order:" + od.orderDistribution[i][j]);
            }
        }

        System.out.println("Length of customer list: " + data.customers.length);
        System.out.println("GiantTour for a period, vehicleType: " + individual.giantTours[0][1].chromosome);
        System.out.println("Periods: " + data.numberOfPeriods);
        System.out.println("Vehicle types: " + data.numberOfVehicleTypes);

         */



    }

}





