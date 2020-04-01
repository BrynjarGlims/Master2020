package Master2020.Individual;

import Master2020.DataFiles.Data;
import Master2020.ProductAllocation.OrderDelivery;
import Master2020.DataFiles.Parameters;

import java.util.*;

public class AdSplit {

    public static Individual individual;
    public static ArrayList<ArrayList<Integer>> matrixOfTrips;




    //MAIN ADSPLIT ALGORITHM------------------------------------------------------------------------------------------------------------------------------------------------------
    public static void adSplitPlural(Individual ind){
        adSplitPlural(ind, 1);
    }

    public static void adSplitPlural(Individual ind, double penaltyMultiplier) {
        resetStaticClass(ind);
        for (int p = 0; p < individual.data.numberOfPeriods; p++) {
            for (int vt = 0; vt < individual.data.numberOfVehicleTypes; vt++) {
                adSplitSingular(individual, p , vt, penaltyMultiplier);
            }
            checkIfAllOrdersAreSatisfied(p);  //todo: to be removed
        }
    }


    public static void adSplitSingular (Individual ind, int p, int vt, double penaltyMultiplier) {
            resetStaticClass(ind);

        if (individual.giantTour.chromosome[p][vt].size() == 0) {
            individual.bestLabels[p][vt] = new Label(ind.data, 0, individual.getOrderDistribution().orderVolumeDistribution, p, vt);
        }
        else{
            //Shortest path algorithm
            createTrips(p, vt, penaltyMultiplier);

            //Labeling algorithm
            labelingAlgorithm(p, vt, matrixOfTrips, penaltyMultiplier);   // Sets bestLabel.

            //Trip generation
            tripAssignment(individual.bestLabels[p][vt], matrixOfTrips);
        }
    }

    public static void adSplitSingular(Individual ind, int p, int vt){
        adSplitSingular(ind, p, vt, 1);
    }



    private static void checkIfAllOrdersAreSatisfied(int p){
        for (OrderDelivery orderDelivery : individual.orderDistribution.orderDeliveries){
            if (orderDelivery.getPeriod() != p)
                continue;
            if (!orderDelivery.dividable){
                if (!individual.tripMap.get(orderDelivery.getPeriod()).containsKey(orderDelivery.order.customerID)){
//                    System.out.println("Missing hashmap: P:" + orderDelivery.getPeriod() + ", C:" + orderDelivery.order.customerID );
//                    System.out.println("Is this combination a valid visit day: " + individual.data.customers[orderDelivery.order.customerID].requiredVisitPeriod[orderDelivery.getPeriod()]);
                }
            }
        }
    }

    private static void tripAssignment( Label label, ArrayList<ArrayList<Integer>> matrixOfTrips){
        int p = label.periodID;
        int vt = label.vehicleTypeID;
        Trip tempTrip;
        Journey tempJourney;
        ArrayList<Journey> journeyList;
        individual.tripList[p][vt]= new ArrayList<Trip>(Arrays.asList(new Trip[matrixOfTrips.size()]));
        journeyList = new ArrayList<Journey>();
        for (LabelEntry labelEntry : label.labelEntries){
            if (!labelEntry.inUse)
                continue;
            tempJourney = new Journey(individual.data, p, vt, labelEntry.vehicleID);
            for (int tripIndex : labelEntry.tripAssigment){
                tempTrip = new Trip();
                tempTrip.initialize(p, vt, labelEntry.vehicleID);
                tempTrip.setCustomers(matrixOfTrips.get(tripIndex));
                tempTrip.setTripIndex(tripIndex);
                tempJourney.addTrip(tempTrip);
                individual.tripList[p][vt].set(tripIndex, tempTrip);
            }
            journeyList.add(tempJourney);
        }
        individual.journeyList[p][vt] = journeyList;
        setTripMap(p, vt);
    }


    private static void setTripMap(int p, int vt){
        for (Trip trip : individual.tripList[p][vt]){
            for (int customerID : trip.customers){
                individual.tripMap.get(p).put(customerID, trip);
            }
        }
    }


    public static void resetStaticClass(Individual ind){
        individual = ind;
        matrixOfTrips = null;
    }

    private static void testTimeWarpValues(ArrayList<ArrayList<Integer>> listOfTrips, ArrayList<Double> arcCost, Data data, int p , int vt ) {
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

    //SHORTEST PATH --------------------------------------------------------------------------------------------------------------------------------------------------------------

    private static void createTrips(int p, int vt){
        createTrips(p, vt, 1);
    }

    private static void createTrips(int p, int vt, double penaltyMultiplier) {
        ArrayList<Integer> customerSequence = (ArrayList<Integer>) individual.giantTour.chromosome[p][vt].clone();  //// TODO: 18.02.2020 Brynjar: is this superfast or only fast?
        //insert depot to be in the 0th position
        customerSequence.add(0, individual.data.customers.length);

        double[] costLabel = getInitialCostLabel(customerSequence.size());
        int[] predecessorLabel = new int[customerSequence.size()];
        double[] timeWarp = new double[customerSequence.size()];
        double loadSum;
        double currentTime;
        double currentCost;
        double tempDistanceCost;
        double routeTimeWarp;


        for (int i = 0; i < customerSequence.size(); i++) {
            for (int j = i+1; j < customerSequence.size(); j++ ) {   //todo: make this a for element in list function.
                loadSum = 0.0;
                currentTime = 0.0;
                tempDistanceCost = 0.0;
                routeTimeWarp = 0.0;

                if (j == (i + 1)) {
                    currentTime += Math.max(individual.data.distanceMatrix[customerSequence.get(0)][customerSequence.get(j)],
                            individual.data.customers[customerSequence.get(j)].timeWindow[p][0]);
                    if (currentTime > individual.data.customers[customerSequence.get(j)].timeWindow[p][1]) {
                        routeTimeWarp += currentTime - individual.data.customers[customerSequence.get(j)].timeWindow[p][1];
                        currentTime = individual.data.customers[customerSequence.get(j)].timeWindow[p][1];
                    }

                    routeTimeWarp += Math.max(0, (currentTime + individual.data.distanceMatrix[customerSequence.get(j)][customerSequence.get(0)]
                            + individual.data.customers[customerSequence.get(j)].totalUnloadingTime) - Parameters.maxJourneyDuration);
                    tempDistanceCost += 2*individual.data.distanceMatrix[customerSequence.get(0)][customerSequence.get(j)];
                    loadSum = individual.orderDistribution.orderVolumeDistribution[p][customerSequence.get(j)];
                    currentCost = routeTimeWarp*Parameters.initialTimeWarpPenalty
                            + Parameters.initialCapacityPenalty*(Math.max(0, loadSum-individual.data.vehicleTypes[vt].capacity));
                    currentCost = currentCost * penaltyMultiplier + Parameters.initialDrivingCostPenalty * tempDistanceCost;
                }

                else {
                    for (int counter = i+1; counter <= j; counter++) {
                        if (counter == i+1){
                            currentTime = Math.max(individual.data.distanceMatrix[customerSequence.get(0)][customerSequence.get(counter)],
                                    individual.data.customers[customerSequence.get(counter)].timeWindow[p][0]);
                            tempDistanceCost += individual.data.distanceMatrix[customerSequence.get(0)][customerSequence.get(counter)];
                        }
                        else {
                            currentTime = Math.max(currentTime + individual.data.distanceMatrix[customerSequence.get(counter-1)][customerSequence.get(counter)],
                                    individual.data.customers[customerSequence.get(counter)].timeWindow[p][0]);
                            tempDistanceCost += individual.data.distanceMatrix[customerSequence.get(counter-1)][customerSequence.get(counter)];
                        }


                        if (currentTime > individual.data.customers[customerSequence.get(counter)].timeWindow[p][1]){
                            routeTimeWarp += currentTime - individual.data.customers[customerSequence.get(counter)].timeWindow[p][1];
                            currentTime = individual.data.customers[customerSequence.get(counter)].timeWindow[p][1];
                        }


                        loadSum += individual.orderDistribution.orderVolumeDistribution[p][customerSequence.get(counter)];

                        //add time warp costs if depot is reached too late
                        if (counter == j) {
                            tempDistanceCost += individual.data.distanceMatrix[customerSequence.get(counter)][customerSequence.get(0)];
                            routeTimeWarp += Math.max(0, (currentTime + individual.data.distanceMatrix[customerSequence.get(counter)][customerSequence.get(0)]
                                    + individual.data.customers[customerSequence.get(counter)].totalUnloadingTime) - Parameters.maxJourneyDuration);
                        }
                    }

                    currentCost = Parameters.initialCapacityPenalty*(Math.max(0, loadSum-individual.data.vehicleTypes[vt].capacity))
                            + routeTimeWarp*Parameters.initialTimeWarpPenalty;
                    currentCost =  currentCost*penaltyMultiplier + Parameters.initialDrivingCostPenalty*tempDistanceCost;
                }

                //Update predecessor label whenever improvements are detected
                if (costLabel[i] + currentCost < costLabel[j]) {
                    costLabel[j] = costLabel[i] + currentCost;
                    timeWarp[j] = routeTimeWarp;
                    predecessorLabel[j] = i;
                }
            }
        }
        getListOfTrips(customerSequence, predecessorLabel, p, vt);
    }

    private static void createTripsAlternative(int p, int vt, double penaltyMultiplier) {
        ArrayList<Integer> customerSequence = (ArrayList<Integer>) individual.giantTour.chromosome[p][vt].clone();  //// TODO: 18.02.2020 Brynjar: is this superfast or only fast?
        //insert depot to be in the 0th position
        customerSequence.add(0, individual.data.customers.length);

        double[] costLabel = getInitialCostLabel(customerSequence.size());
        int[] predecessorLabel = new int[customerSequence.size()];
        double[] timeWarp = new double[customerSequence.size()];

        for (int i = 0; i < customerSequence.size(); i++) {
            for (int j = i+1; j < customerSequence.size(); j++ ) {   //todo: make this a for element in list function.
                double loadSum = 0.0;
                double currentTime = 0.0; //Always 0 as all trips starts in the depot
                double currentCost = 0.0;
                double tempDistanceCost = 0.0;
                double routeTimeWarp = 0.0;
                if (j == (i + 1)) { //direct route from depot to j
                    currentTime += Math.max(individual.data.distanceMatrix[customerSequence.get(0)][customerSequence.get(j)], individual.data.customers[customerSequence.get(j)].timeWindow[p][0]);
                    routeTimeWarp += Math.max(0, (currentTime - individual.data.customers[customerSequence.get(j)].timeWindow[p][1]));
                    //add time warp costs if depot is reached too late

                    if ((currentTime - individual.data.customers[customerSequence.get(j)].timeWindow[p][1]) > 0) {
                        currentTime = individual.data.customers[customerSequence.get(j)].timeWindow[p][1];
                    }
                    routeTimeWarp += Math.max(0, (currentTime + individual.data.distanceMatrix[customerSequence.get(j)][customerSequence.get(0)] + individual.data.customers[customerSequence.get(j)].totalUnloadingTime) - Parameters.maxJourneyDuration);
                    tempDistanceCost += individual.data.distanceMatrix[customerSequence.get(0)][customerSequence.get(j)] + individual.data.distanceMatrix[customerSequence.get(j)][customerSequence.get(0)];
                    loadSum = individual.orderDistribution.orderVolumeDistribution[p][customerSequence.get(j)];

                    currentCost = (tempDistanceCost)*Parameters.initialDrivingCostPenalty
                            + routeTimeWarp*Parameters.initialTimeWarpPenalty
                            + Parameters.initialCapacityPenalty*(Math.max(0, loadSum-individual.data.vehicleTypes[vt].capacity));
                    currentCost *= penaltyMultiplier;
                }

                else if (j != (i+1)) {
                    //add driving time from depot to the first customer
                    double tempTime = individual.data.distanceMatrix[customerSequence.get(0)][customerSequence.get(i+1)];
                    //add driving distance from depot to the first node
                    tempDistanceCost += individual.data.distanceMatrix[customerSequence.get(0)][customerSequence.get(i+1)];

                    for (int counter = i+1; counter <= j; counter++) {
                        //add driving distance from prev node
                        if (counter > i+1) {
                            tempTime += individual.data.distanceMatrix[customerSequence.get(counter-1)][customerSequence.get(counter)]
                                    + individual.data.customers[customerSequence.get(counter)].totalUnloadingTime;;
                            tempDistanceCost += individual.data.distanceMatrix[customerSequence.get(counter-1)][customerSequence.get(counter)];
                        }

                        //update current time and calculate time warp
                        currentTime = Math.max(individual.data.customers[customerSequence.get(counter)].timeWindow[p][0], tempTime);
                        routeTimeWarp += Math.max(0, (currentTime - individual.data.customers[customerSequence.get(counter)].timeWindow[p][1]));
                        if ((currentTime - individual.data.customers[customerSequence.get(counter)].timeWindow[p][1]) > 0){
                            currentTime = individual.data.customers[customerSequence.get(counter)].timeWindow[p][1];
                        }

                        loadSum += individual.orderDistribution.orderVolumeDistribution[p][customerSequence.get(counter)];

                        //add time warp costs if depot is reached too late
                        if (counter == j) {
                            tempDistanceCost += individual.data.distanceMatrix[customerSequence.get(counter)][customerSequence.get(0)];
                            routeTimeWarp += Math.max(0, (currentTime + individual.data.distanceMatrix[customerSequence.get(counter)][customerSequence.get(0)] + individual.data.customers[customerSequence.get(counter)].totalUnloadingTime) - Parameters.maxJourneyDuration);
                        }
                    }

                    currentCost = Parameters.initialCapacityPenalty*(Math.max(0, loadSum-individual.data.vehicleTypes[vt].capacity))
                            + routeTimeWarp*Parameters.initialTimeWarpPenalty + Parameters.initialDrivingCostPenalty*(tempDistanceCost);
                    currentCost *= penaltyMultiplier;
                }
                //Update predecessor label whenever improvements are detected
                if (costLabel[i] + currentCost < costLabel[j]) {
                    costLabel[j] = costLabel[i] + currentCost;
                    timeWarp[j] = routeTimeWarp;
                    predecessorLabel[j] = i;
                }
            }
        }
        System.out.println("Stop");
        getListOfTrips(customerSequence, predecessorLabel, p, vt);
    }



    private static ArrayList<ArrayList<Integer>> getListOfTrips(ArrayList<Integer> customerSequence, int[] predecessorLabel, int p, int vt) {
        ArrayList<ArrayList<Integer>> listOfTrips = new ArrayList<ArrayList<Integer>>();
        int currentNode = predecessorLabel.length -1;

        while (currentNode > 0) {
            ArrayList<Integer> tempListOfTrips = new ArrayList<Integer>();
            if (predecessorLabel[currentNode] == currentNode - 1) {
                tempListOfTrips.add(customerSequence.get(currentNode));
                listOfTrips.add(0,tempListOfTrips);
                currentNode--;
            } else {
                for (int count = predecessorLabel[currentNode] +1; count <= currentNode; count++) {
                    tempListOfTrips.add(customerSequence.get(count));
                }
                listOfTrips.add(0,tempListOfTrips);
                currentNode = predecessorLabel[currentNode];
            }
        }
        matrixOfTrips = listOfTrips;
        return listOfTrips;
    }



    //LABELING------------------------------------------------------------------------------------------------------------------------------------------------------------
    private static void labelingAlgorithm(int p, int vt, ArrayList<ArrayList<Integer>> listOfTrips, double penaltyMultiplier) {

        int tripNumber = 0;
        LabelPool currentLabelPool = new LabelPool(individual.data, listOfTrips, tripNumber, individual.orderDistribution.orderVolumeDistribution);
        LabelPool nextLabelPool;

        while(tripNumber < listOfTrips.size()) {
            if (tripNumber == 0) {
                currentLabelPool.generateFirstLabel(individual.data.numberOfVehiclesInVehicleType[vt], p, vt, penaltyMultiplier);
                tripNumber++;
            } else {
                nextLabelPool = new LabelPool(individual.data, listOfTrips, tripNumber, individual.orderDistribution.orderVolumeDistribution);
                nextLabelPool.generateAndRemoveDominatedLabels(currentLabelPool, penaltyMultiplier);
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
        Arrays.fill(costLabel, Double.MAX_VALUE);
        costLabel[0] = 0;
        return costLabel;
    }

    private static int[] getInitialPredecessorLabel(int size){
        int[] predecessorLabel =  new int[size];
        Arrays.fill(predecessorLabel, 0);
        return predecessorLabel;
    }

}