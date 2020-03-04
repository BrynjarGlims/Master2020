package Individual;
import DataFiles.Data;
import DataFiles.DataReader;
import ProductAllocation.OrderDistribution;
import DataFiles.Parameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class AdSplit {

    public static Individual individual;
    public static ArrayList<ArrayList<Integer>> matrixOfTrips;
    public static ArrayList<Double> matrixOfTripCosts;



    //MAIN ADSPLIT ALGORITHM------------------------------------------------------------------------------------------------------------------------------------------------------

    public static void adSplitSingular (Individual ind, int p, int vt, boolean reset) {
        if (reset){
            resetStaticClass(ind);
        }
        if (individual.giantTour.chromosome[p][vt].size() == 0) {
            individual.bestLabels[p][vt] = new Label(ind.data, 0, individual.orderDistribution.orderVolumeDistribution, p, vt);
            individual.vehicleAssigment.setChromosome(new HashMap<Integer, Integer>(), p);
            //Set giantTourSplit
            individual.giantTourSplit.setChromosome(new ArrayList<Integer>(), p, vt);
        }
        else{
            //Shortest path algorithm
            createTrips(p, vt);

            //DEBUG:
            //testTimeWarpValues(matrixOfTrips, matrixOfTripCosts, individual.data, p , vt );

            //Labeling algorithm
            labelingAlgorithm(p, vt, matrixOfTrips);   // Sets bestLabel.
            //Set vehicleAssignment
            individual.vehicleAssigment.setChromosome(getVehicleAssignmentChromosome(p, vt), p);
            //Set giantTourSplit
            individual.giantTourSplit.setChromosome(createSplitChromosome(matrixOfTrips), p, vt);
        }

    }

    public static void adSplitSingular (Individual ind, int p, int vt) {
        adSplitSingular(ind, p, vt, true);
    }

    public static void resetStaticClass(Individual ind){
        individual = ind;
        matrixOfTrips = null;
        matrixOfTripCosts = null;
    }

    public static void testTimeWarpValues(ArrayList<ArrayList<Integer>> listOfTrips, ArrayList<Double> arcCost, Data data, int p , int vt ) {
        boolean fromDepot = true;
        int lastCustomerID = -1;
        double currentVehicleTime = 0;
        double timeWarpInfeasibility = 0;

        //todo: make more readable
        for (ArrayList<Integer> trip : listOfTrips) {
            for (int customerID : trip) {
                if (fromDepot) {  //depot to customer
                    currentVehicleTime = Math.max(currentVehicleTime + data.distanceMatrix[data.numberOfCustomers][customerID],
                            data.customers[customerID].timeWindow[p][0]);
                    if (currentVehicleTime > data.customers[customerID].timeWindow[p][1]) {
                        timeWarpInfeasibility += currentVehicleTime - data.customers[customerID].timeWindow[p][1];
                        currentVehicleTime = data.customers[customerID].timeWindow[p][1];
                    }
                    lastCustomerID = customerID;
                    fromDepot = false;
                } else {  //Case where one goes from customer to customer
                    currentVehicleTime = Math.max(currentVehicleTime + data.customers[customerID].totalUnloadingTime + data.distanceMatrix[lastCustomerID][customerID],
                            data.customers[customerID].timeWindow[p][0]);
                    if (currentVehicleTime > data.customers[customerID].timeWindow[p][1]) {
                        timeWarpInfeasibility += currentVehicleTime - data.customers[customerID].timeWindow[p][1];
                        currentVehicleTime = data.customers[customerID].timeWindow[p][1];
                    }
                    lastCustomerID = customerID;
                }
            }
            currentVehicleTime += data.customers[lastCustomerID].totalUnloadingTime +
                    data.distanceMatrix[lastCustomerID][data.numberOfCustomers];
            if (currentVehicleTime > Parameters.maxJourneyDuration) {
                timeWarpInfeasibility += currentVehicleTime - Parameters.maxJourneyDuration;
                currentVehicleTime = Parameters.maxJourneyDuration;
            }

            System.out.println("Time warp inf: " + timeWarpInfeasibility);

            fromDepot = true;
            lastCustomerID = -1;
            currentVehicleTime = 0;
            timeWarpInfeasibility = 0;
        }
    }

    public static void adSplitPlural(Individual ind) {
        resetStaticClass(ind);
        for (int p = 0; p < individual.data.numberOfPeriods; p++) {
            for (int vt = 0; vt < individual.data.numberOfVehicleTypes; vt++) {
                adSplitSingular(individual, p , vt, false);
                System.out.println(individual.isFeasible());
            }
        }
    }

    //SHORTEST PATH --------------------------------------------------------------------------------------------------------------------------------------------------------------

    private static void createTrips(int p, int vt) {
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


        for (int i = 0; i < customerSequence.size(); i++) {
            for (int j = i+1; j < customerSequence.size(); j++ ) {   //todo: make this a for element in list function.
                double loadSum = 0.0;
                double currentTime = 0.0; //Always 0 as all trips starts in the depot
                double currentCost = 0.0;
                double tempDistanceCost = 0.0;
                double routeTimeWarp = 0.0;
                System.out.println("Considering trip: (" +i+","+j+")" );

                if (j == (i + 1)) { //direct route from depot to j
                    currentTime += individual.data.distanceMatrix[customerSequence.get(0)][customerSequence.get(j)];
                    routeTimeWarp += Math.max(0, (currentTime - individual.data.customers[customerSequence.get(j)].timeWindow[p][1]));

                    tempDistanceCost += individual.data.distanceMatrix[individual.data.customers.length][customerSequence.get(j)]; //drive to depot
                    loadSum = individual.orderDistribution.orderVolumeDistribution[p][customerSequence.get(j)];

                    currentCost = (tempDistanceCost)*Parameters.initialDrivingCostPenalty
                            + routeTimeWarp*Parameters.initialTimeWarpPenalty + Parameters.initialCapacityPenalty*(Math.max(0, loadSum-individual.data.vehicleTypes[vt].capacity));

                } else if (j != (i+1)) {
                    //add driving time from depot to the first customer
                    double tempTime = individual.data.distanceMatrix[customerSequence.get(0)][customerSequence.get(i+1)];
                    //add driving distance from depot to the first node
                    tempDistanceCost += individual.data.distanceMatrix[customerSequence.get(0)][customerSequence.get(i+1)];

                    for (int counter = i; counter <= j; counter++) {
                        //add driving distance from prev node
                        if (counter > i) {
                            tempTime += individual.data.distanceMatrix[customerSequence.get(counter-1)][customerSequence.get(counter)];
                        }

                        //Add unloading time for all nodes except from depot and the last node on the trip
                        //update current time and calculate time warp
                        if (counter != 0) {
                            currentTime = Math.max(individual.data.customers[customerSequence.get(counter)].timeWindow[p][0], tempTime);
                            routeTimeWarp += Math.max(0, (currentTime - individual.data.customers[customerSequence.get(counter)].timeWindow[p][1]));
                        }

                        //add loadsum if node is not depot
                        //add unloading time at customer before time warp at next customer is calculated
                        if (counter != 0) {
                            loadSum += individual.orderDistribution.orderVolumeDistribution[p][customerSequence.get(counter)];
                            tempTime += individual.data.customers[customerSequence.get(counter)].totalUnloadingTime;
                        }
                    }
                    currentCost = Parameters.initialCapacityPenalty*(Math.max(0, loadSum-individual.data.vehicleTypes[vt].capacity))
                            + routeTimeWarp*Parameters.initialTimeWarpPenalty + Parameters.initialDrivingCostPenalty*(tempDistanceCost);
                }

                //Update predecessor label whenever improvements are detected
                if (costLabel[i] + currentCost < costLabel[j]) {
                    System.out.println("Improvement found");
                    costLabel[j] = costLabel[i] + currentCost;
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
        //System.out.println("Customer sequence: "+ customerSequence);

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


    private static HashMap<Integer, Integer> getVehicleAssignmentChromosome(int p, int vt){
        HashMap<Integer, Integer> hashMap = new HashMap<Integer, Integer>();
        if (individual.giantTour.chromosome[p][vt].size()==0 ) {
            return hashMap;
        }
        for (LabelEntry labelEntry : individual.bestLabels[p][vt].labelEntries) {
            for (ArrayList<Integer> customerList : labelEntry.tripAssigment){
                for (int customerID : customerList) {
                    hashMap.put(customerID, labelEntry.vehicleID);  // TODO: 24.02.2020 Change to correct vehicle id
                }
            }
        }
        return hashMap;
    }



    private static ArrayList<Integer> createSplitChromosome(ArrayList<ArrayList<Integer>> customerSequence) {
        ArrayList<Integer> splits = new ArrayList<>();
        int split = 0;
        if (customerSequence.isEmpty()){
            return splits;
        }

        for (ArrayList<Integer> tripList : customerSequence) {
            split += tripList.size();
            splits.add(split);
        }
        return splits;
    }

    //LABELING------------------------------------------------------------------------------------------------------------------------------------------------------------
    private static void labelingAlgorithm(int p, int vt, ArrayList<ArrayList<Integer>> listOfTrips) {

        int tripNumber = 0;
        LabelPool currentLabelPool = new LabelPool(individual.data, listOfTrips, tripNumber, individual.orderDistribution.orderVolumeDistribution);
        LabelPool nextLabelPool;
        //System.out.println("    ");
        //System.out.println("----------Number of trips to be combined: " + listOfTrips.size() + "  -------");
        //System.out.println("Number of vehicles: " + individual.data.vehicleTypes[vt].vehicleSet.size());

        while(tripNumber < listOfTrips.size()) {
            if (tripNumber == 0) {
                currentLabelPool.generateFirstLabel(individual.data.numberOfVehiclesInVehicleType[vt], p, vt);
                tripNumber++;
            } else {
                nextLabelPool = new LabelPool(individual.data, listOfTrips, tripNumber, individual.orderDistribution.orderVolumeDistribution);
                nextLabelPool.generateAndRemoveDominatedLabels(currentLabelPool);
                //System.out.println("Number of labels after removal: " + nextLabelPool.labels.size());
                currentLabelPool = nextLabelPool;
                tripNumber++;
            }
        }
        if (currentLabelPool.labels.size() > 0){
            individual.bestLabels[p][vt] = currentLabelPool.findBestLabel();
        }
    }

    private static double[] getInitialCostLabel(int size){
        double[] costLabel =  new double[size];
        Arrays.fill(costLabel, 100000);
        costLabel[0] = 0;
        return costLabel;
    }

    private static int[] getInitialPredecessorLabel(int size){
        int[] predecessorLabel =  new int[size];
        Arrays.fill(predecessorLabel, 0);
        return predecessorLabel;
    }

}








