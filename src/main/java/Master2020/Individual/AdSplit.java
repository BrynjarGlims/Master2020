package Master2020.Individual;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.Parameters;
import Master2020.ProductAllocation.OrderDistribution;
import scala.xml.PrettyPrinter;

import java.util.*;

public class AdSplit {

    //public static Individual individual;
    //public static ArrayList<ArrayList<Integer>> matrixOfTrips;



    //MAIN ADSPLIT ALGORITHM------------------------------------------------------------------------------------------------------------------------------------------------------
    public static void adSplitPlural(Individual individual){
        adSplitPlural(individual, 1);
    }

    public static void adSplitPlural(Individual individual, double penaltyMultiplier) {
        for (int p = 0; p < individual.numberOfPeriods; p++) {
            if (Parameters.isPeriodic){
                p = individual.actualPeriod;
            }
            for (int vt = 0; vt < individual.data.numberOfVehicleTypes; vt++) {
                adSplitSingular(individual, p, vt, penaltyMultiplier);  //should have the actual period!!!
            }
        }
    }



    public static void adSplitSingular (Individual individual, int p, int vt, double penaltyMultiplier){
        if (individual.giantTour.chromosome[Individual.getDefaultPeriod(p)][vt].size() == 0) {
            individual.journeyList[Individual.getDefaultPeriod(p)][vt] = new ArrayList<Journey>();
            individual.tripList[Individual.getDefaultPeriod(p)][vt] = new ArrayList<Trip>();
        }
        else{
            //Shortest path algorithm
            ArrayList<ArrayList<Integer>> matrixOfTrips = createTrips(individual.giantTour.chromosome[Individual.getDefaultPeriod(p)][vt], individual.data, individual.orderDistribution, individual.getActualPeriod(p), vt, penaltyMultiplier);

            //Labeling algorithm
            Label bestLabel  = labelingAlgorithm(matrixOfTrips, individual.data, individual.orderDistribution, individual.getActualPeriod(p), vt, matrixOfTrips, penaltyMultiplier);   // Sets bestLabel.

            //Trip generation
            ArrayList<Journey> journeyList = tripAssignment(bestLabel, matrixOfTrips, individual.data);

            updateIndividual(individual, journeyList, matrixOfTrips, Individual.getDefaultPeriod(p), vt);

        }
    }

    private static void updateIndividual(Individual individual, ArrayList<Journey> journeyList, ArrayList<ArrayList<Integer>> matrixOfTrips, int p, int vt){

        // set journey list
        individual.journeyList[p][vt] = journeyList;

        // set trip list
        individual.tripList[p][vt]= new ArrayList<Trip>(Arrays.asList(new Trip[matrixOfTrips.size()]));
        for (Journey journey : journeyList){
            for (Trip trip : journey.trips){
                individual.tripList[p][vt].set(trip.tripIndex, trip);
            }
        }
        // fill trip map
        setTripMap(individual, p, vt);
    }

    public static ArrayList<Journey> adSplitSingular(ArrayList<Integer> giantTour, Data data, OrderDistribution orderDistribution, int p, int vt) {
        return adSplitSingular(giantTour, data, orderDistribution, p, vt, 1);
    }




    // ------------- NEW ADSPLIT ---------------------
    public static ArrayList<Journey> adSplitSingular(ArrayList<Integer> giantTour, Data data,
                                                     OrderDistribution orderDistribution, int p, int vt, double penaltyMultiplier){
        if (giantTour.size() == 0) {
            return new ArrayList<Journey>();
        }
        else{
            //Shortest path algorithm
            ArrayList<ArrayList<Integer>> matrixOfTrips = createTrips(giantTour, data, orderDistribution,  p, vt, penaltyMultiplier);

            //Labeling algorithm
            Label bestLabel = labelingAlgorithm(matrixOfTrips, data, orderDistribution, p, vt, matrixOfTrips, penaltyMultiplier);   // Sets bestLabel.

            //Trip generation
            ArrayList<Journey> journeyList = tripAssignment(bestLabel, matrixOfTrips, data);

            return journeyList;
        }
    }



    public static void adSplitSingular(Individual ind, int p, int vt){
        adSplitSingular(ind, p, vt, 1);
    }


    private static ArrayList<Journey> tripAssignment(Label label, ArrayList<ArrayList<Integer>> matrixOfTrips, Data data){
        int p = label.periodID;
        int vt = label.vehicleTypeID;
        Trip tempTrip;
        Journey tempJourney;
        ArrayList<Journey> journeyList  = new ArrayList<Journey>();
        for (LabelEntry labelEntry : label.labelEntries){
            if (!labelEntry.inUse)
                continue;
            tempJourney = new Journey(data, p, vt, labelEntry.vehicleID);
            for (int tripIndex : labelEntry.tripAssigment){
                tempTrip = new Trip();
                tempTrip.initialize(Individual.getDefaultPeriod(p), vt, labelEntry.vehicleID);
                tempTrip.setCustomers(matrixOfTrips.get(tripIndex));
                tempTrip.setTripIndex(tripIndex);
                tempJourney.addTrip(tempTrip);
            }
            journeyList.add(tempJourney);
        }

        return journeyList;

    }


    private static void setTripMap(Individual individual, int p, int vt){
        for (Trip trip : individual.tripList[p][vt]){  //todo: is setTripMap at any time reseted?
            for (int customerID : trip.customers){
                individual.tripMap.get(p).put(customerID, trip);
            }
        }
    }




    //SHORTEST PATH --------------------------------------------------------------------------------------------------------------------------------------------------------------

    private static void createTrips(ArrayList<Integer> giantTour, Data data, OrderDistribution orderDistribution, int  p, int vt){
        createTrips(giantTour, data, orderDistribution, p, vt, 1);
    }

    private static ArrayList<ArrayList<Integer>> createTrips(ArrayList<Integer> giantTour, Data data, OrderDistribution orderDistribution, int p, int vt, double penaltyMultiplier) {
        ArrayList<Integer> customerSequence = (ArrayList<Integer>) giantTour.clone();  //// TODO: 18.02.2020 Brynjar: is this superfast or only fast?
        //insert depot to be in the 0th position
        customerSequence.add(0, data.customers.length);

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
                    currentTime += Math.max(data.distanceMatrix[customerSequence.get(0)][customerSequence.get(j)],
                            data.customers[customerSequence.get(j)].timeWindow[p][0]);
                    if (currentTime > data.customers[customerSequence.get(j)].timeWindow[p][1]) {
                        routeTimeWarp += currentTime - data.customers[customerSequence.get(j)].timeWindow[p][1];
                        currentTime = data.customers[customerSequence.get(j)].timeWindow[p][1];
                    }

                    routeTimeWarp += Math.max(0, (currentTime + data.distanceMatrix[customerSequence.get(j)][customerSequence.get(0)]
                            + data.customers[customerSequence.get(j)].totalUnloadingTime) - Parameters.maxJourneyDuration);
                    tempDistanceCost += 2*data.distanceMatrix[customerSequence.get(0)][customerSequence.get(j)];
                    loadSum = orderDistribution.getOrderVolumeDistribution(p,customerSequence.get(j));
                    currentCost = routeTimeWarp*Parameters.initialTimeWarpPenalty
                            + Parameters.initialCapacityPenalty*(Math.max(0, loadSum-data.vehicleTypes[vt].capacity));
                    currentCost = currentCost * penaltyMultiplier + Parameters.initialDrivingCostPenalty * tempDistanceCost;
                }

                else {
                    for (int counter = i+1; counter <= j; counter++) {
                        if (counter == i+1){
                            currentTime = Math.max(data.distanceMatrix[customerSequence.get(0)][customerSequence.get(counter)],
                                    data.customers[customerSequence.get(counter)].timeWindow[p][0]);
                            tempDistanceCost += data.distanceMatrix[customerSequence.get(0)][customerSequence.get(counter)];
                        }
                        else {
                            currentTime = Math.max(currentTime + data.distanceMatrix[customerSequence.get(counter-1)][customerSequence.get(counter)],
                                    data.customers[customerSequence.get(counter)].timeWindow[p][0]);
                            tempDistanceCost += data.distanceMatrix[customerSequence.get(counter-1)][customerSequence.get(counter)];
                        }


                        if (currentTime > data.customers[customerSequence.get(counter)].timeWindow[p][1]){
                            routeTimeWarp += currentTime - data.customers[customerSequence.get(counter)].timeWindow[p][1];
                            currentTime = data.customers[customerSequence.get(counter)].timeWindow[p][1];
                        }


                        loadSum += orderDistribution.getOrderVolumeDistribution(p,customerSequence.get(counter));

                        //add time warp costs if depot is reached too late
                        if (counter == j) {
                            tempDistanceCost += data.distanceMatrix[customerSequence.get(counter)][customerSequence.get(0)];
                            routeTimeWarp += Math.max(0, (currentTime + data.distanceMatrix[customerSequence.get(counter)][customerSequence.get(0)]
                                    + data.customers[customerSequence.get(counter)].totalUnloadingTime) - Parameters.maxJourneyDuration);
                        }
                    }

                    currentCost = Parameters.initialCapacityPenalty*(Math.max(0, loadSum-data.vehicleTypes[vt].capacity))
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
        return getListOfTrips(customerSequence, predecessorLabel, p, vt);
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
        return listOfTrips;
    }



    //LABELING------------------------------------------------------------------------------------------------------------------------------------------------------------
    private static Label labelingAlgorithm(ArrayList<ArrayList<Integer>> matrixOfTrips, Data data, OrderDistribution orderDistribution, int p, int vt, ArrayList<ArrayList<Integer>> listOfTrips, double penaltyMultiplier) {

        int tripNumber = 0;
        LabelPool currentLabelPool = new LabelPool(data, listOfTrips, tripNumber, orderDistribution.getOrderVolumeDistribution());
        LabelPool nextLabelPool;

        while(tripNumber < listOfTrips.size()) {
            if (tripNumber == 0) {
                currentLabelPool.generateFirstLabel(data.numberOfVehiclesInVehicleType[vt], p, vt, penaltyMultiplier);
                tripNumber++;
            } else {
                nextLabelPool = new LabelPool(data, listOfTrips, tripNumber, orderDistribution.getOrderVolumeDistribution());
                nextLabelPool.generateAndRemoveDominatedLabels(currentLabelPool, penaltyMultiplier);
                currentLabelPool = nextLabelPool;
                tripNumber++;
            }
        }
        if (currentLabelPool.labels.size() > 0){
            return currentLabelPool.findBestLabel();
        }
        else
            return null;   // TODO: 21/04/2020 Check if this is problematic
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