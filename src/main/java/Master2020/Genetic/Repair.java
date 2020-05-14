package Master2020.Genetic;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.Individual.Individual;
import Master2020.Individual.AdSplit;
import Master2020.ProductAllocation.OrderDistribution;

import Master2020.DataFiles.*;

import java.util.Arrays;

public class Repair {


    public static boolean repair(Individual individual, OrderDistribution orderDistribution, PenaltyControl penaltyControl){
        int i;
        for (i = 1 ; i < 4 ; i++){
            double penaltyMultiplier = Math.pow(10,i);
            AdSplit.adSplitPlural(individual, penaltyMultiplier, penaltyControl.timeWarpPenalty, penaltyControl.overLoadPenalty);
            Education.improveRoutes(individual, orderDistribution, penaltyMultiplier, penaltyControl.timeWarpPenalty, penaltyControl.overLoadPenalty);
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
    