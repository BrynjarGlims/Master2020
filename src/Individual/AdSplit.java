package Individual;
import DataFiles.Data;
import DataFiles.DataReader;
import ProductAllocation.OrderDistribution;
import DataFiles.Parameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdSplit {

    public static Individual individual;
    public static ArrayList<ArrayList<Integer>> matrixOfTrips;
    public static ArrayList<Double> matrixOfTripCosts;



    //MAIN ADSPLIT ALGORITHM------------------------------------------------------------------------------------------------------------------------------------------------------

    public static void adSplitSingular (Individual ind, int p, int vt) {
        individual = ind;
        matrixOfTrips = null;
        matrixOfTripCosts = null;
        if (!(individual.giantTour.chromosome[p][vt].size() == 0)) {
            //Shortest path algorithm
            createTrips(p, vt);
            //Labeling algorithm
            labelingAlgorithm(p, vt, matrixOfTrips, matrixOfTripCosts);   // Sets bestLabel.

            // TODO: 18.02.2020 Implement an improved split procedure that reorders customers
            //Set vehicleAssignment
            individual.vehicleAssigment.setChromosome(individual.bestLabels[p][vt].getVehicleAssignmentChromosome(), p, vt);
            //Set giantTourSplit
            individual.giantTourSplit.setChromosome(createSplitChromosome(matrixOfTrips), p, vt);
        }

    }

    public static void adSplitPlural(Individual ind) {
        individual = ind;
        matrixOfTrips = null;
        matrixOfTripCosts = null;
        for (int p = 0; p < individual.data.numberOfPeriods; p++) {
            for (int vt = 0; vt < individual.data.numberOfVehicleTypes; vt++) {
                if (individual.giantTour.chromosome[p][vt].size() == 0) {
                    continue;
                }
                //Shortest path algorithm
                createTrips(p, vt);
                //Labeling algorithm
                labelingAlgorithm(p, vt, matrixOfTrips, matrixOfTripCosts);   // Sets bestLabel.

                // TODO: 18.02.2020 Implement an improved split procedure that reorders customers
                //Set vehicleAssignment
                individual.vehicleAssigment.setChromosome(individual.bestLabels[p][vt].getVehicleAssignmentChromosome(), p, vt);
                //Set giantTourSplit
                individual.giantTourSplit.setChromosome(createSplitChromosome(matrixOfTrips), p, vt);
            }
        }
    }

    //SHORTEST PATH --------------------------------------------------------------------------------------------------------------------------------------------------------------

    public static void createTrips(int p, int vt) {
        ArrayList<Integer> customerSequence = (ArrayList<Integer>) individual.giantTour.chromosome[p][vt].clone();  //// TODO: 18.02.2020 Brynjar: is this superfast or only fast?
        //insert depot to be in the 0th position
        customerSequence.add(0, individual.data.customers.length);

        double[] costLabel = getInitialCostLabel(customerSequence.size());
        int[] predecessorLabel = new int[customerSequence.size()];

        /*
        double[] timeOfArrival = new double[customerSequence.size()];
        //initialize time of arrival with distance from depot
        timeOfArrival[0] = 0.0;
        for (int i = 1; i < timeOfArrival.length; i++) {
            timeOfArrival[i] = individual.data.distanceMatrix[individual.data.customers.length][customerSequence.get(i)];
        }
         */

        double routeTimeWarp = 0.0;

        for (int i = 0; i < customerSequence.size(); i++) {
            for (int j = i+1; j < customerSequence.size(); j++ ) {   //todo: make this a for element in list function.
                double loadSum = 0.0;
                double currentTime = 0.0; //Always 0 as all trips starts in the depot
                double currentCost = 0.0;
                double tempDistanceCost = 0.0;

                if (j == (i + 1)) {
                    if (customerSequence.get(i) == individual.data.customers.length) {
                        currentTime += individual.data.distanceMatrix[customerSequence.get(i)][customerSequence.get(j)];
                    }
                    else {
                        currentTime += individual.data.distanceMatrix[customerSequence.get(i)][customerSequence.get(j)] + individual.data.customers[customerSequence.get(i)].totalUnloadingTime;
                    }
                    routeTimeWarp = Math.max(0, (currentTime - individual.data.customers[customerSequence.get(j)].timeWindow[p][1]));
                    tempDistanceCost += individual.data.distanceMatrix[customerSequence.get(i)][customerSequence.get(j)] + individual.data.distanceMatrix[customerSequence.get(j)][individual.data.customers.length];
                    loadSum = individual.orderDistribution.orderDistribution[p][customerSequence.get(j)];

                    currentCost = (tempDistanceCost)*Parameters.initialDrivingCostPenalty
                            + routeTimeWarp*Parameters.initialTimeWarpPenalty + Parameters.initialCapacityPenalty*(Math.max(0, loadSum-individual.data.vehicleTypes[vt].capacity));

                } else {
                    //add driving time from depot to the first customer
                    double tempTime = currentTime + individual.data.distanceMatrix[customerSequence.get(0)][customerSequence.get(j)];

                    //add driving distance from last node to depot, and from depot to first node
                    tempDistanceCost += individual.data.distanceMatrix[customerSequence.get(j)][customerSequence.get(0)]
                            + individual.data.distanceMatrix[customerSequence.get(0)][customerSequence.get(j)];

                    for (int counter = i+1; counter <= j; counter++) {
                        //not for first customer (no unloading time at depot)
                        if (!(customerSequence.get(counter-1) == individual.data.customers.length)) {
                            tempTime += individual.data.distanceMatrix[customerSequence.get(counter - 1)][customerSequence.get(counter)]
                                    + individual.data.customers[customerSequence.get(counter - 1)].totalUnloadingTime;
                        }
                        currentTime = Math.max(individual.data.customers[customerSequence.get(counter)].timeWindow[p][0], tempTime);
                        routeTimeWarp += Math.max(0, (currentTime - individual.data.customers[customerSequence.get(j)].timeWindow[p][1]));
                        if (counter != i+1) {
                            tempDistanceCost += individual.data.distanceMatrix[customerSequence.get(counter - 1)][customerSequence.get(counter)];
                        }
                        loadSum += individual.orderDistribution.orderDistribution[p][customerSequence.get(counter)];
                    }
                    currentCost += Parameters.initialCapacityPenalty*(Math.max(0, loadSum-individual.data.vehicleTypes[vt].capacity))
                            + routeTimeWarp*Parameters.initialTimeWarpPenalty + Parameters.initialDrivingCostPenalty*(individual.data.distanceMatrix[customerSequence.get(j - 1)][customerSequence.get(j)]);
                }

                //Update predecessor label whenever improvements are detected
                if (costLabel[i] + currentCost < costLabel[j]) {
                    costLabel[j] = costLabel[i] + currentCost;
                    //timeOfArrival[j] = Math.max(individual.data.customers[customerSequence.get(j)].timeWindow[p][0], timeOfArrival[i]
                    // + individual.data.distanceMatrix[customerSequence.get(i)][customerSequence.get(j)]);
                    predecessorLabel[j] = i;
                }
            }
        }
        extractVrpSolution(customerSequence, predecessorLabel, p, vt);
    }


    private static void extractVrpSolution(ArrayList<Integer> customerSequence, int[] predecessorLabel, int p, int vt) {
        //extract VRP solution by backtracking the shortest path label

        ArrayList<ArrayList<Integer>> listOfTrips = setListOfTrips(customerSequence, predecessorLabel);
        double tripCost;
        //Calculate trip costs
        ArrayList<Double> listOfTripCosts = new ArrayList<Double>(listOfTrips.size());
        for (List<Integer> list : listOfTrips) {
            tripCost = 0;
            if (list.size() == 1) {
                tripCost += individual.data.distanceMatrix[individual.data.customers.length][list.get(0)] + individual.data.distanceMatrix[list.get(list.size() - 1)][individual.data.customers.length];
            }
            if (list.size() > 1) {
                tripCost += individual.data.distanceMatrix[individual.data.customers.length][list.get(0)] + individual.data.distanceMatrix[list.get(list.size() - 1)][individual.data.customers.length];
                for (int i = 1; i < list.size() - 1; i++) {
                    tripCost += individual.data.distanceMatrix[list.get(i)][list.get(i + 1)];
                }
            }
            listOfTripCosts.add(tripCost);
        }
        matrixOfTrips = listOfTrips;
        matrixOfTripCosts = listOfTripCosts;
    }

    private static ArrayList<ArrayList<Integer>> setListOfTrips(ArrayList<Integer> customerSequence, int[] predecessorLabel) {
        ArrayList<ArrayList<Integer>> listOfTrips = new ArrayList<ArrayList<Integer>>();
        ArrayList<Integer> tempListOfTrips = new ArrayList<Integer>();
        System.out.println("Customer sequence: "+ customerSequence);

        if (predecessorLabel.length == 2) {
            tempListOfTrips.add(0,customerSequence.get(1));
            listOfTrips.add(0,tempListOfTrips);
        }

        else if (predecessorLabel.length > 2) {
            int currentNodeIndex = customerSequence.size()-1;
            int numberOfAddedCustomers = 0;

            while (numberOfAddedCustomers != (customerSequence.size()-1)) {
                if (predecessorLabel[currentNodeIndex] == 0) {
                    tempListOfTrips.add(0, customerSequence.get(currentNodeIndex));
                    numberOfAddedCustomers += 1;
                    listOfTrips.add(0,tempListOfTrips);
                    tempListOfTrips = new ArrayList<Integer>();
                    currentNodeIndex -=1;
                }
                else {
                    for (int i = currentNodeIndex; i > predecessorLabel[currentNodeIndex]; i--) {
                        tempListOfTrips.add(0,customerSequence.get(i));
                        numberOfAddedCustomers += 1;
                    }
                    listOfTrips.add(0,tempListOfTrips);
                    tempListOfTrips = new ArrayList<Integer>();
                    currentNodeIndex = predecessorLabel[currentNodeIndex];
                }
            }
        }
        System.out.println("list of trips added: " + listOfTrips);
        return listOfTrips;
    }

    /*
            for (int k = customerSequence.size() - 1; k > 0; k--) {
                System.out.println("k: "+k);
                if (predecessorLabel[k] == 0) {
                    listOfTrips.add(tempListOfTrips);
                    tempListOfTrips = new ArrayList<>();
                    tempListOfTrips.add(customerSequence.get(k));
                }
                else {
                    int counter = predecessorLabel[k];
                    while (k ) {
                        k = predecessorLabel[k-1];
                    for (int counter = k; counter > predecessorLabel[k]; counter--) {

                    }
                        if (!currentAddedList.contains(customerSequence.get(counter))) {
                            if (predecessorLabel[k] == 0) {
                                listOfTrips.add(tempListOfTrips);
                                //tempListOfTrips = new ArrayList<>();
                            } else {
                                tempListOfTrips.add(0, customerSequence.get(counter));
                                currentAddedList.add(customerSequence.get(counter));
                                System.out.println("templist added customer: " + customerSequence.get(counter));
                            }
                        }
                    }
                }
                System.out.println("--------------------");
            }listOfTrips.add(tempListOfTrips);

            /*
            for (int k = 2; k < customerSequence.size(); k++) {
                if (predecessorLabel[k] == 0) {
                    listOfTrips.add(tempListOfTrips);
                    tempListOfTrips = new ArrayList<>();
                    tempListOfTrips.add(customerSequence.get(k));
                }
                else if (predecessorLabel[k] == k-1) {
                    tempListOfTrips.add(customerSequence.get(k));
                }
                if (k == (customerSequence.size()-1)) {
                    listOfTrips.add(tempListOfTrips);
                }
            }
        }
            return listOfTrips;
        }
        */

    public static ArrayList<Integer> createSplitChromosome(ArrayList<ArrayList<Integer>> customerSequence) {
        ArrayList<Integer> splits = new ArrayList<>();
        int split = 0;

        for (ArrayList<Integer> tripList : customerSequence) {
            split += tripList.size();
            splits.add(split);
        }
        return splits;
    }

    //LABELING------------------------------------------------------------------------------------------------------------------------------------------------------------
    public static void labelingAlgorithm(int p, int vt, ArrayList<java.util.ArrayList<Integer>> listOfTrips, ArrayList<Double> arcCost) {

        int tripNumber = 0;
        LabelPool currentLabelPool = new LabelPool(individual.data, listOfTrips, tripNumber, individual.orderDistribution.orderDistribution);
        LabelPool nextLabelPool;

        while(tripNumber < listOfTrips.size()) {
            if (tripNumber == 0) {
                currentLabelPool.generateFirstLabel(individual.data.numberOfVehiclesInVehicleType[vt],
                        arcCost.get(tripNumber), p, vt);
                tripNumber++;
            } else {
                nextLabelPool = new LabelPool(individual.data, listOfTrips, tripNumber, individual.orderDistribution.orderDistribution);
                nextLabelPool.generateLabels(currentLabelPool, arcCost.get(tripNumber));
                nextLabelPool.removeDominated();
                currentLabelPool = nextLabelPool;
                tripNumber++;
            }
        }
        if (currentLabelPool.labels.size() > 0){
            individual.bestLabels[p][vt] = currentLabelPool.findBestLabel();
        }
    }

    public static double[] getInitialCostLabel(int size){
        double[] costLabel =  new double[size];
        Arrays.fill(costLabel, 100000);
        costLabel[0] = 0;
        return costLabel;
    }

    public static int[] getInitialPredecessorLabel(int size){
        int[] predecessorLabel =  new int[size];
        Arrays.fill(predecessorLabel, 0);
        return predecessorLabel;
    }

}








