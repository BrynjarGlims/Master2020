package Genetic;

import java.util.*;

import DataFiles.*;
import Individual.Individual;
import ProductAllocation.OrderDistribution;
import ProjectReport.Journey;


public class FitnessCalculation {   // TODO: 26.02.2020 Se if this can remove parts of code in LabelEntryClass

    public static double getTripFitness(List<Integer> customerOrder, int vt, int p, double[][] orderDistribution, Data data){
        double fitness = 0;
        fitness += overloadScore(customerOrder, vt, p, orderDistribution, data);
        fitness += travelingDistanceScoreAndOvertimeScore(customerOrder, vt, p, orderDistribution, data);
        fitness += timeWarpScore(customerOrder, vt, p, orderDistribution, data);
        return fitness;
    }


    //-----------------------------------------Order distribution fitness based on filling level------------------------------------------
    public static double getFitnessForAnIndividualAndAnOrderDistribution (Individual individual, OrderDistribution orderDistribution) {
        double totalFitness = 0;
        for (int p = 0; p < Parameters.numberOfPeriods; p++) {
            double periodicFitness = 0;
            for (int vt = 0; vt < individual.data.numberOfVehicleTypes; vt++) {
                periodicFitness += getSingleChromosomeFitness(vt, p, individual, orderDistribution);
            }
            totalFitness += periodicFitness;
        }
        System.out.println("For individual: " + individual.giantTour.chromosome + ", orderDistr: "+ orderDistribution.hashCode() + ", fitness: "+ totalFitness);
        return totalFitness;
    }

    public static double getSingleChromosomeFitness(int vt, int p, Individual individual, OrderDistribution orderDistribution) {
        double tripLoad = 0;
        double singleChromosomeFitness = 0;
        if (!individual.giantTourSplit.chromosome[p][vt].isEmpty()) {
            Iterator iterator = individual.giantTourSplit.chromosome[p][vt].iterator();
            int split = (Integer) iterator.next();
            /*
            System.out.println("------------");
            System.out.println("giant tour: " + individual.giantTour.chromosome[p][vt]);
            System.out.println("giant tour split: " + individual.giantTourSplit.chromosome[p][vt]);
             */
            for (int i = 0; i < individual.giantTour.chromosome[p][vt].size(); i++) {
                tripLoad += orderDistribution.orderVolumeDistribution[p][individual.giantTour.chromosome[p][vt].get(i)];
                if (i == split-1) {
                    singleChromosomeFitness += calculateJourneyLoadPunishment(tripLoad, vt, individual);
                    tripLoad = 0;
                    if (i != individual.giantTour.chromosome[p][vt].size() - 1) {
                        if (iterator.hasNext()) {
                            //if there are more splits
                            split = (Integer) iterator.next();
                        }
                        else {
                            // split will never be reached
                            split = individual.giantTour.chromosome[p][vt].size();
                        }
                    }
                }
            }
        /*
        for (int tripSplitIndex = 0; tripSplitIndex < individual.giantTourSplit.chromosome[p][vt].size(); tripSplitIndex++) {
            from = tripSplitIndex - 1 == -1 ? 0 : individual.giantTourSplit.chromosome[p][vt].get(tripSplitIndex - 1);
            currentTrip = new LinkedList<>(individual.giantTour.chromosome[p][vt].subList(from, individual.giantTourSplit.chromosome[p][vt].get(tripSplitIndex)));
            for (int customerID: currentTrip) {
                tripLoad += orderDistribution.orderVolumeDistribution[p][customerID];
            }
        }
        */
        }

        return singleChromosomeFitness;
    }

    public static double calculateJourneyLoadPunishment(double tripLoad, int vt, Individual individual) {
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

    //---------------------------------------------------------------------------------------------------------------------------------------------

    //-----------------------------------------Depot overtime fitness score------------------------------------------------------------------------
    /*
    //TODO 2.3: implement calculations
    public static double depotOvertimeScoreInPeriod (List<Integer> customerOrder, int p, double[][] orderDistribution,  Data data) {
        double overtime = 0;
        double loadSumForVehicleType = 0;
        int tripLoad = 0;
        for (int vt = 0; vt < data.numberOfVehicleTypes; vt++) {
            //TODO 2.3: calculate currentLoad
            for (all trips : c) {
                tripLoad+=;
            }
            loadSumForVehicleType += Math.max(0, tripLoad - 0);
        }

        return overtime*Parameters.initialOvertimePenalty;
    }
     */

    private static double overloadScore(List<Integer> customerOrder, int vt, int p, double[][] orderDistribution,  Data data){
        double load = 0;
        for (int customerID : customerOrder){
            load += orderDistribution[p][customerID];
        }
        return Math.max(0, load - data.vehicleTypes[vt].capacity*Parameters.initialCapacityPenalty);
    }


    private static double timeWarpScore(List<Integer> customerOrder, int vt, int p, double[][] orderDistribution,  Data data){
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
            currentVehicleTime = Parameters.maxJourneyDuration;
        }
        return timeWarpInfeasibility * Parameters.initialTimeWarpPenalty;
    }

    private static double travelingDistanceScoreAndOvertimeScore(List<Integer> customerOrder, int vt, int p, double[][] orderDistribution,  Data data){
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
        vehicleTotalTravelTime +=
                data.distanceMatrix[lastCustomerID][data.numberOfCustomers];
        vehicleDrivingDistance += data.distanceMatrix[lastCustomerID][data.numberOfCustomers];
        vehicleTotalTravelTime *= Parameters.initialOvertimePenalty;
        vehicleDrivingDistance *= Parameters.initialDrivingCostPenalty;
        return vehicleDrivingDistance + vehicleTotalTravelTime;
    }


}
