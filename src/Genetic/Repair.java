package Genetic;

import DataFiles.Customer;
import DataFiles.Data;
import DataFiles.DataReader;
import Individual.Individual;
import Individual.AdSplit;
import ProductAllocation.OrderDistribution;
import Individual.*;
import Individual.LabelEntry;

import DataFiles.*;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class Repair {


    public static boolean repair(Individual individual, OrderDistribution orderDistribution){
        int i;
        for (i = 1 ; i < 4 ; i++){
            double penaltyMultiplier = Math.pow(10,i);
            System.out.println("INDIVIDUAL PRIOR:");
            System.out.println(individual);
            AdSplit.adSplitPlural(individual, penaltyMultiplier);
            Education.improveRoutes(individual, orderDistribution, penaltyMultiplier);
            System.out.println("EDUCATED");
            System.out.println(individual);

            individual.updateFitness();
            System.out.println("IS FEASIBLE: " + individual.isFeasible() + " infeasibility cost: " + individual.infeasibilityCost);
            System.out.println("time warp: " + individual.infeasibilityTimeWarpValue + "\ncapacity: " + individual.infeasibilityOverCapacityValue
                + "\ndriving time: " + individual.infeasibilityOvertimeDrivngValue);
            for (int p = 0 ; p < individual.data.numberOfPeriods ; p++){
                for (int vt =  0 ; vt < individual.data.numberOfVehicleTypes ; vt++){
                    System.out.println("best label for period " + p + " vehicletype " + vt + " timewarp: " + individual.bestLabels[p][vt].fleetTimeWarp);
                    for (LabelEntry le : individual.bestLabels[p][vt].labelEntries){
                        System.out.println("vehicle: " + le.vehicleID + " has timewarp: " + le.timeWarpInfeasibility + " and current time: " + le.currentVehicleTime);
                    }
                }
            }
            for (int p = 0 ; p < individual.data.numberOfPeriods ; p++){
                for (int vt =  0 ; vt < individual.data.numberOfVehicleTypes ; vt++){
                    for (Trip trip : individual.tripList[p][vt]){
                        System.out.println("vehicle: " + trip.vehicleID + " takes trip " + trip.tripIndex + " in period " + p);
                        for (int c : trip.customers){
                            System.out.println("customer " + c + " has timewindow: " + Arrays.toString(individual.data.customers[c].timeWindow[p]));
                        }
                    }
                }
            }


                    if (individual.isFeasible()){
                return true;
            }
        }
        individual.updateFitness(1.0/Math.pow(10,i));
        return false;
    }


    public static void main(String[] args){
        Data data = DataReader.loadData();
        for (double[] row : data.distanceMatrix){
            System.out.println(Arrays.toString(row));
        }
        for (VehicleType v : data.vehicleTypes){
            System.out.println("type: " + v.vehicleTypeID + " #vehicles: " + v.loadingTimeAtDepot);
        }

    }


}
    