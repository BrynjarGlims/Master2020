package Genetic;

import DataFiles.Data;
import DataFiles.DataReader;
import Individual.Individual;
import Individual.AdSplit;
import ProductAllocation.OrderDistribution;

import DataFiles.*;

import java.util.Arrays;

public class Repair {


    public static boolean repair(Individual individual, OrderDistribution orderDistribution){
        int i;
        for (i = 1 ; i < 4 ; i++){
            double penaltyMultiplier = Math.pow(10,i);
            AdSplit.adSplitPlural(individual, penaltyMultiplier);
            Education.improveRoutes(individual, orderDistribution, penaltyMultiplier);
            individual.updateFitness();
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
    