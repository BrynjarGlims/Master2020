package Individual;
import DataFiles.*;
import ProductAllocation.OrderDistribution;

import javax.lang.model.type.ArrayType;
import java.util.ArrayList;
import java.util.Arrays;

public class Individual {


    //chromosome data
    public GiantTour giantTours;  //period, vehicleType
    public VehicleAssigment vehicleAssigment;
    public GiantTourSplit giantTourSplit;
    public VehicleType vehicleType;
    public double costOfIndividual;
    public ArrayList<ArrayList<Integer>>[][] listOfTrips;  //array1: period, array2: vehicletype
    public double[][] arcCostMatrix;
    public OrderDistribution orderDistribution;
    public ArrayList<LabelPool> labelPools;

    public Data data;
    public int[][] arcCost;  // (i,j) i = from, j = to

    public LabelPool[][] lastLabelPool;
    public Label[][] bestLabel;

    public Individual(Data data, OrderDistribution orderDistribution) {
        this.data = data;
        this.orderDistribution = orderDistribution;
        this.costOfIndividual = costOfIndividual;
        this.listOfTrips = new ArrayList[data.numberOfPeriods][data.numberOfVehicleTypes];

        this.giantTours = new GiantTour(data);
        this.arcCostMatrix = new double[Parameters.numberOfCustomers][Parameters.numberOfCustomers];
        this.lastLabelPool = new LabelPool[data.numberOfPeriods][data.numberOfVehicleTypes];
        this.bestLabel = new Label[data.numberOfPeriods][data.numberOfVehicleTypes];

        this.vehicleAssigment = new VehicleAssigment(data);
        this.giantTourSplit = new GiantTourSplit(data);


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


    /*
    public int[] createTrips(int p, int vt) {
        ArrayList<Integer> customerSequence = this.giantTours[p][vt].chromosome; // TODO: 13.02.2020 MAYBY p -1 and vt -1, merge conflict

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

     */


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


    public void labelingAlgorithm(int p, int vt, ArrayList<ArrayList<Integer>> listOfTrips, ArrayList<Integer> arcCost){

        // take this as input,remove afterwards
        this.labelPools = new ArrayList<LabelPool>();  //reset the previous label pool


        for (int tripNumber = 0; tripNumber < listOfTrips.size(); tripNumber++){
            if (tripNumber == 0){
                LabelPool newLabelPool = new LabelPool(data, listOfTrips, tripNumber, orderDistribution.orderDistribution);
                newLabelPool.generateFirstLabel(data.numberOfVehiclesInVehicleType[vt],
                        arcCost.get(tripNumber), p, vt);
                labelPools.add(newLabelPool);
                System.out.println(newLabelPool);
            }
            else{
                LabelPool newLabelPool = new LabelPool(data, listOfTrips, tripNumber, orderDistribution.orderDistribution);
                newLabelPool.generateLabels(labelPools.get(labelPools.size()-1), arcCost.get(tripNumber));
                System.out.println(newLabelPool);
                newLabelPool.removeDominated();
                labelPools.add(newLabelPool);
                System.out.println(newLabelPool);
            }
        }

        this.lastLabelPool[p][vt] = labelPools.get(labelPools.size()-1);
        this.bestLabel[p][vt] = lastLabelPool[p][vt].findBestLabel();
    }







    //solves for each period
    public void adSplit() {
        for (int p = 0; p < data.numberOfPeriods; p++) {
            for (int vt = 0; vt < this.data.numberOfVehicleTypes; vt++) {
                // int[] tripCost = createTrips(p, vt);  //Fride implements, returns int[] with arc cost in correct order
                System.out.println(data.numberOfVehiclesInVehicleType[vt]);
                ArrayList<Integer> arcCost = new ArrayList<Integer>(Arrays.asList(34,54,23));

                ArrayList<ArrayList<Integer>> customerSequence = new ArrayList<ArrayList<Integer>>();

                customerSequence.add(new ArrayList<Integer>(Arrays.asList(2,3)));
                customerSequence.add(new ArrayList<Integer>(Arrays.asList(1,4)));
                customerSequence.add(new ArrayList<Integer>(Arrays.asList(0)));

                labelingAlgorithm(p, vt, customerSequence, arcCost);   // Wrong initialization

                //Set vehicleAssignment
                vehicleAssigment.setChromosome(bestLabel[p][vt].getVehicleAssignmentList(), p,vt);
                //Set giantTourSplit
                giantTourSplit.setChromosome(createSplitChromosome(customerSequence), p, vt );
                break;

            }
            break;
        }
    }


    public ArrayList<Integer> createSplitChromosome(ArrayList<ArrayList<Integer>> customerSequence){
        ArrayList<Integer> splits = new ArrayList<Integer>();
        int split = 0;

        for (ArrayList<Integer> tripList: customerSequence){
            split += tripList.size();
            splits.add(split);
        }
        return splits;

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
        individual.adSplit();
        //individual.showBestSplit();
        //individual.createTrips(data.numberOfPeriods, data.numberOfVehicleTypes);



        System.out.println("hei");




    }

}





