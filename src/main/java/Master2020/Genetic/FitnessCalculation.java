package Master2020.Genetic;

import java.util.*;

import Master2020.DataFiles.*;
import Master2020.Individual.Individual;
import Master2020.ProductAllocation.OrderDistribution;
import Master2020.Individual.Trip;
import Master2020.Individual.Journey;


public class FitnessCalculation {   // TODO: 26.02.2020 Se if this can remove parts of code in LabelEntryClass


    public static double[] getIndividualFitness(Individual individual, double penaltyMultiplier){
        return getIndividualFitness(individual.data, individual.journeyList, individual.orderDistribution, penaltyMultiplier);
    }

    public static double[] getIndividualFitness(Data data, ArrayList<Journey>[][] journeys, OrderDistribution orderDistribution, double penaltyMultiplier){
        double travelCost = 0;
        double timeWarpCost = 0;
        double overloadCost = 0;
        double vehicleUsageCost = 0;
        double[] fitnesses;
        for (int p = 0 ; p < journeys.length ; p++){
            for (int vt = 0 ; vt < data.numberOfVehicleTypes ; vt++){
                for (Journey journey : journeys[p][vt]){
                    if (journey == null){
                        System.out.println("journey is null");
                        continue;
                    }
                    if (journey.trips.size() == 0){
                        System.out.println("No trips in journey");
                    }
                    fitnesses = getJourneyFitness(journey, orderDistribution, penaltyMultiplier);
                    travelCost += fitnesses[0];
                    timeWarpCost += fitnesses[1];
                    overloadCost += fitnesses[2];
                    vehicleUsageCost += fitnesses[3];
                }
            }
        }
        return new double[]{travelCost,timeWarpCost,overloadCost,vehicleUsageCost};
    }


    public static double getTripFitness(List<Integer> customerOrder, int vt, int p, double[][] orderDistribution, Data data){
        return getTripFitness(customerOrder, vt, p, orderDistribution, data, 1);
    }

    public static double getTripFitness(List<Integer> customerOrder, int vt, int p, double[][] orderDistribution, Data data, double penaltyMultiplier){
        double fitness = 0;
        if (customerOrder.isEmpty()){
            return 0;
        }
        fitness += overloadScore(customerOrder, vt, p, orderDistribution, data, penaltyMultiplier);
        fitness += travelingDistanceScoreAndOvertimeScore(customerOrder, vt, p, orderDistribution, data, penaltyMultiplier);
        fitness += timeWarpScore(customerOrder, vt, p, data, penaltyMultiplier);
        return fitness;
    }

    public static double getTotalJourneyFitness(Journey journey, OrderDistribution orderDistribution){
        return getTotalJourneyFitness(journey, orderDistribution, 1);
    }

    public static double getTotalJourneyFitness(Journey journey, OrderDistribution orderDistribution, double penaltyMultiplier){
        double[] fitnesses = getJourneyFitness(journey, orderDistribution, penaltyMultiplier);
        return fitnesses[0] + fitnesses[1] + fitnesses[2];
    }


    public static double[] getJourneyFitness(Journey journey, OrderDistribution orderDistribution){
        return getJourneyFitness(journey, orderDistribution, 1);
    }

    public static double[] getJourneyFitness(Journey journey, OrderDistribution orderDistribution, double penaltyMultiplier){
        return journey.updateFitness(orderDistribution, penaltyMultiplier);
    }


    //-----------------------------------------Order distribution fitness based on filling level------------------------------------------
    public static double getFillLevelFitnessAnIndividualAndAnOrderDistribution(Individual individual, OrderDistribution orderDistribution) {
        double totalFitness = 0;
        for (int p = 0; p < Parameters.numberOfPeriods; p++) {
            double periodicFitness = 0;
            for (int vt = 0; vt < individual.data.numberOfVehicleTypes; vt++) {
                periodicFitness += getSingleChromosomeFitness(vt, p, individual, orderDistribution);
            }
            totalFitness += periodicFitness;
        }
        return totalFitness;
    }

    private static double getSingleChromosomeFitness(int vt, int p, Individual individual, OrderDistribution orderDistribution) {
        double tripLoad = 0;
        double singleChromosomeFitness = 0;
        if (!individual.tripList[p][vt].isEmpty()) {
            for (Trip trip : individual.tripList[p][vt]){
                for (int customerID : trip.getCustomers()){
                    tripLoad += orderDistribution.getOrderVolumeDistribution(p,customerID);
                }
                singleChromosomeFitness += calculateJourneyLoadPunishment(tripLoad, vt, individual);
                tripLoad = 0;
            }
        }
        return singleChromosomeFitness;
    }

    private static double calculateJourneyLoadPunishment(double tripLoad, int vt, Individual individual) {
        double fitness = 0;
        if (tripLoad > individual.data.vehicleTypes[vt].capacity) {
            fitness = Parameters.penaltyFactorForOverFilling*((tripLoad - individual.data.vehicleTypes[vt].capacity)/individual.data.vehicleTypes[vt].capacity);
        }
        else if (tripLoad < individual.data.vehicleTypes[vt].capacity){
            fitness = Parameters.penaltyFactorForUnderFilling*((individual.data.vehicleTypes[vt].capacity - tripLoad)/individual.data.vehicleTypes[vt].capacity);
        }
        else if (tripLoad == individual.data.vehicleTypes[vt].capacity) {
            fitness = 0;
        }
        return fitness;
    }


    public static double getPeriodicOvertimeFitness (OrderDistribution orderDistribution, int p) {
        double periodicOvertime = 0;
        periodicOvertime = Math.max(orderDistribution.getVolumePerPeriod(p) - Data.overtimeLimit[p], 0);
        return periodicOvertime*Parameters.overtimeCost[p];
    }

//    public static double getIndividualOvertimeFitness (OrderDistribution orderDistribution) {
//        double overtimeFitness = 0;
//        for (int p = 0; p < Parameters.numberOfPeriods; p++) {
//            overtimeFitness += getPeriodicOvertimeFitness(orderDistribution, p);
//        }
//        return overtimeFitness;
//    }
//

    private static double overTimeDepot(OrderDistribution orderDistribution){
        double overTime = 0;
        for (int p = 0 ; p < Parameters.numberOfPeriods ; p++){
            overTime += Math.max(0, orderDistribution.getVolumePerPeriod(p) - Data.overtimeLimit[p])*Parameters.overtimeCost[p];
        }
        return overTime;
    }

    private static double overloadScore(List<Integer> customerOrder, int vt, int p, double[][] orderDistribution,  Data data, double penaltyMultiplier){
        double load = 0;
        for (int customerID : customerOrder){
            load += orderDistribution[p][customerID];
        }
        return Math.max(0, load - data.vehicleTypes[vt].capacity*penaltyMultiplier*Parameters.initialCapacityPenalty);
    }


    private static double timeWarpScore(List<Integer> customerOrder, int vt, int p,  Data data, double penaltyMultiplier){
        boolean fromDepot = true;
        int lastCustomerID = -1;
        double currentVehicleTime = 0;
        double timeWarpInfeasibility = 0;

        //todo: make more readable

        for (int customerID : customerOrder){
            if (fromDepot){  //depot to customer
                currentVehicleTime = Math.max(currentVehicleTime + data.distanceMatrix[data.numberOfCustomers][customerID],
                        data.customers[customerID].timeWindow[p][0]);
                if (currentVehicleTime > data.customers[customerID].timeWindow[p][1]){
                    timeWarpInfeasibility +=  currentVehicleTime - data.customers[customerID].timeWindow[p][1];
                    currentVehicleTime = data.customers[customerID].timeWindow[p][1];
                }
                lastCustomerID = customerID;
                fromDepot = false;
            }
            else{  //Case where one goes from customer to customer
                currentVehicleTime = Math.max(currentVehicleTime + data.customers[customerID].totalUnloadingTime +data.distanceMatrix[lastCustomerID][customerID],
                        data.customers[customerID].timeWindow[p][0]);
                if (currentVehicleTime > data.customers[customerID].timeWindow[p][1]){
                    timeWarpInfeasibility +=  currentVehicleTime - data.customers[customerID].timeWindow[p][1];
                    currentVehicleTime = data.customers[customerID].timeWindow[p][1];
                }
                lastCustomerID = customerID;
            }
        }
        currentVehicleTime +=  data.customers[lastCustomerID].totalUnloadingTime +
                data.distanceMatrix[lastCustomerID][data.numberOfCustomers];
        if (currentVehicleTime > Parameters.maxJourneyDuration){
            timeWarpInfeasibility += currentVehicleTime - Parameters.maxJourneyDuration;
        }
        return timeWarpInfeasibility * penaltyMultiplier * Parameters.initialTimeWarpPenalty;
    }

    private static double travelingDistanceScoreAndOvertimeScore(List<Integer> customerOrder, int vt, int p, double[][] orderDistribution,  Data data, double penaltyMultiplier){
        //initialize
        int customerCounter = 0;
        int lastCustomerID = -1;
        double vehicleTotalTravelTime = 0;
        double vehicleDrivingDistance = 0;

        //three cases, from depot to cust, cust to cust, cust to depot
        for ( int customerID : customerOrder){
            if (customerCounter == 0){
                vehicleTotalTravelTime += data.distanceMatrix[data.numberOfCustomers][customerID]+
                        data.customers[customerID].totalUnloadingTime;
                vehicleDrivingDistance = data.distanceMatrix[data.numberOfCustomers][customerID];
                lastCustomerID = customerID;
                customerCounter++;
            }
            else {
                vehicleTotalTravelTime += data.distanceMatrix[lastCustomerID][customerID] +
                        data.customers[customerID].totalUnloadingTime;
                vehicleDrivingDistance += data.distanceMatrix[lastCustomerID][customerID];
                lastCustomerID = customerID;
                customerCounter++;
            }
        }
        vehicleTotalTravelTime += data.distanceMatrix[lastCustomerID][data.numberOfCustomers]; // not in use
        vehicleDrivingDistance += data.distanceMatrix[lastCustomerID][data.numberOfCustomers];
        vehicleDrivingDistance *= Parameters.initialDrivingCostPenalty;
        return vehicleDrivingDistance;
    }


}
