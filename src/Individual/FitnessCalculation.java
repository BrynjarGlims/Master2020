package Individual;

import java.util.ArrayList;
import DataFiles.*;


public class FitnessCalculation {   // TODO: 26.02.2020 Se if this can remove parts of code in LabelEntryClass

    public static double getTripFitness(ArrayList<Integer> customerOrder, int vt, int p, double[][] orderDistribution, Data data){
        double fitness = 0;
        fitness += overloadScore(customerOrder, vt, p, orderDistribution, data);
        fitness += travelingDistanceScoreAndOvertimeScore(customerOrder, vt, p, orderDistribution, data);
        fitness += timeWarpScore(customerOrder, vt, p, orderDistribution, data);
        return fitness;


    }

    private static double overloadScore(ArrayList<Integer> customerOrder, int vt, int p, double[][] orderDistribution,  Data data){
        double load = 0;
        for (int customerID : customerOrder){
            load += orderDistribution[p][customerID];
        }
        return Math.max(0, load - data.vehicleTypes[vt].capacity*Parameters.initialCapacityPenalty);

    }

    private static double timeWarpScore(ArrayList<Integer> customerOrder, int vt, int p, double[][] orderDistribution,  Data data){
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
        return timeWarpInfeasibility* Parameters.initialTimeWarpPenalty;




    }

    private static double travelingDistanceScoreAndOvertimeScore(ArrayList<Integer> customerOrder, int vt, int p, double[][] orderDistribution,  Data data){
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
