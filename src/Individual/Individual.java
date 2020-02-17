package Individual;
import DataFiles.*;
import ProductAllocation.OrderDistribution;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class Individual {
    public GiantTour giantTour;  //period, vehicleType
    // TODO: The giantTour class will be a 2D array, s.t. one chromosome represents giant tours for all (period, vehicleType)
    public VehicleAssigment[][] vehicleAssigment;
    public GiantTourSplit[][] giantTourSplits;
    public VehicleType vehicleType;
    public ArrayList<ArrayList<Integer>>[][] matrixOfTrips;
    public OrderDistribution orderDistribution;
    public ArrayList<Double>[][] matrixOfTripCosts;

    public Data data;
    public int[][] arcCost;  // (i,j) i = from, j = to

    public LabelPool[][] lastLabelPool;
    public Label[][] bestLabel;

    public Individual(Data data, OrderDistribution orderDistribution) {
        this.data = data;
        this.orderDistribution = orderDistribution;
        this.giantTour = new GiantTour(data);
        this.matrixOfTrips = new ArrayList[data.numberOfPeriods][data.numberOfVehicleTypes];
        this.matrixOfTripCosts = new ArrayList[data.numberOfPeriods][data.numberOfVehicleTypes];
        this.lastLabelPool = new LabelPool[data.numberOfPeriods][data.numberOfVehicleTypes];
        this.bestLabel = new Label[data.numberOfPeriods][data.numberOfVehicleTypes];
        this.vehicleAssigment = new VehicleAssigment[data.numberOfPeriods][data.numberOfVehicleTypes];
        this.giantTourSplits = new GiantTourSplit[data.numberOfPeriods][data.numberOfVehicleTypes];
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


    //------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    //SHORTEST PATH METHODS:

    public void createTrips(int p, int vt) {
        //SHORTEST PATH
        ArrayList<Integer> customerSequence = this.giantTour.chromosome[p][vt];

        //insert depot to be in the 0th position
        customerSequence.add(0, data.customers.length);
        double[] costLabel = new double[customerSequence.size()];
        int[] predecessorLabel = new int[customerSequence.size()];


        for (int i = 0; i < customerSequence.size(); i++) {
            costLabel[i] = 100000;
            predecessorLabel[i] = 0;
        }
        costLabel[0] = 0;
        for (int i = 0; i < customerSequence.size(); i++) {
            double loadSum = 0;
            double distanceCost = 0;
            int j = i+1;
            while (j < customerSequence.size()) {
                loadSum += this.orderDistribution.orderDistribution[p][customerSequence.get(j)];
                if (j == (i + 1)) {
                    distanceCost = data.distanceMatrix[customerSequence.get(j)][data.customers.length];
                } else {
                    distanceCost = data.distanceMatrix[customerSequence.get(j - 1)][customerSequence.get(j)] + data.distanceMatrix[customerSequence.get(j)][data.customers.length];
                }
                if (costLabel[i] + distanceCost < costLabel[j] && loadSum <= data.vehicleTypes[vt].capacity) {
                    costLabel[j] = costLabel[i] + distanceCost;
                    predecessorLabel[j] = i;

                }
                j += 1;
            }
        }
        System.out.println("Predecessors: ");
        for (int i = 0; i < predecessorLabel.length; i++) {
            System.out.println(predecessorLabel[i]);
        }
        System.out.println();
        extractVrpSolution(customerSequence, predecessorLabel, p, vt);
    }
    public void extractVrpSolution(ArrayList<Integer> customerSequence, int[] predecessorLabel, int p, int vt) {
        //extract VRP solution by backtracking the shortest path label
        ArrayList<ArrayList<Integer>> listOfTrips = new ArrayList<ArrayList<Integer>>();
        ArrayList<Integer> tempListOfTrips = new ArrayList<Integer>();

    //TODO: last element in giantTour often neglected. why? debug.
        for (int k = 1; k < customerSequence.size(); k++) {
            if (k==1) {
                tempListOfTrips.add(customerSequence.get(k));
            }
            else if (predecessorLabel[k] == (k-1)) {
                //only add customers, not the depot, to a trip
                tempListOfTrips.add(customerSequence.get(k - 1));
            }

            else if (predecessorLabel[k] == 0) {
                tempListOfTrips.add(customerSequence.get(k));
                //System.out.println("tempList added to listOfTrips: " + tempListOfTrips);
                listOfTrips.add(tempListOfTrips);
                tempListOfTrips = new ArrayList<>();
                //System.out.println("cleared, tempList = "+ tempListOfTrips);
            }
        }

        //Calculate trip costs
        ArrayList<Double> listOfTripCosts = new ArrayList<Double>(listOfTrips.size());
        for (List<Integer> list : listOfTrips) {
            double tripCost = 0;
            if (list.size() == 1){
                tripCost += data.distanceMatrix[data.customers.length][list.get(0)] + data.distanceMatrix[list.get(list.size() - 1)][data.customers.length];
            }
            if (list.size() > 1) {
                tripCost += data.distanceMatrix[data.customers.length][list.get(0)] + data.distanceMatrix[list.get(list.size() - 1)][data.customers.length];
                for (int i = 1; i < list.size() - 1; i++) {
                    tripCost += data.distanceMatrix[list.get(i)][list.get(i + 1)];
                }
            }
            listOfTripCosts.add(tripCost);
        }
        this.matrixOfTrips[p][vt] = listOfTrips;
        this.matrixOfTripCosts[p][vt] = listOfTripCosts;
        System.out.println("(Period, Vehicle Type) = (" + p +", "+vt+")");
        System.out.println("Giant Tour (beginning with depot): " + customerSequence);
        System.out.println("List of trips:" + listOfTrips);
        System.out.println("List of costs: " + listOfTripCosts);
        System.out.println("----------------------------------");
    }

    //------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------



/*

    public void labelingAlgorithm(int p, int vt, ArrayList<ArrayList<Integer>> listOfTrips, ArrayList<Integer> arcCost) {

        // take this as input,remove afterwards
        ArrayList<LabelPool> labelPools = new ArrayList<LabelPool>();

        for (int tripNumber = 0; tripNumber < listOfTrips.size(); tripNumber++) {
            if (tripNumber == 0) {
                LabelPool newLabelPool = new LabelPool(data, listOfTrips, tripNumber, orderDistribution.orderDistribution);
                newLabelPool.generateFirstLabel(data.numberOfVehiclesInVehicleType[vt],
                        arcCost.get(tripNumber));
                labelPools.add(newLabelPool);
            } else {
                LabelPool newLabelPool = new LabelPool(data, listOfTrips, tripNumber, orderDistribution.orderDistribution);
                newLabelPool.generateLabels(labelPools.get(labelPools.size() - 1), arcCost.get(tripNumber));
                newLabelPool.removeDominated();
                labelPools.add(newLabelPool);
            }
        }
        this.lastLabelPool[p][vt] = labelPools.get(labelPools.size() - 1);
        this.bestLabel[p][vt] = lastLabelPool[p][vt].findBestLabel();
    }


    //solves for each period
    public void adSplit() {
        for (int p = 0; p < data.numberOfPeriods; p++) {
            for (int vt = 0; vt < this.data.numberOfVehicleTypes; vt++) {
                // int[] tripCost = createTrips(p, vt);  //Fride implements, returns int[] with arc cost in correct order

                ArrayList<Integer> arcCost = new ArrayList<Integer>(Arrays.asList(34, 54, 23));

                ArrayList<ArrayList<Integer>> customerSequence = new ArrayList<ArrayList<Integer>>();

                customerSequence.add(new ArrayList<Integer>(Arrays.asList(2, 3)));
                customerSequence.add(new ArrayList<Integer>(Arrays.asList(1, 4)));
                customerSequence.add(new ArrayList<Integer>(Arrays.asList(0)));
                labelingAlgorithm(p, vt, customerSequence, arcCost);   // Wrong initialization
                createChromosomeFromBestLabel(p, vt);

            }
        }
    }

    public void createChromosomeFromBestLabel(int p, int vt) {
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

    }

         */
    public static void main(String[] args){
        Data data = DataReader.loadData();
        OrderDistribution od = new OrderDistribution(data);
        od.makeDistribution();
        Individual individual = new Individual(data, od);
        //for all (period, vehicleType)
        for (int p = 0; p < data.numberOfPeriods; p++) {
            for (int vt = 0; vt < data.numberOfVehicleTypes; vt++) {
                individual.createTrips(p, vt);
            }
        }

        //individual.adSplit();
        //individual.showBestSplit();

        /*
        //Print the resulting matrix attributes
        for (int p = 0; p < data.numberOfPeriods; p++) {
            for (int vt = 0; vt < data.numberOfVehicleTypes; vt++) {
                System.out.println("(Period, Vehicle Type) = (" + p +", "+vt+")");
                System.out.println("TripCost: "+ individual.matrixOfTripCosts[p][vt]);
                System.out.println("TripList: " + individual.matrixOfTrips[p][vt]);
            }
        }

         */

    }


}








