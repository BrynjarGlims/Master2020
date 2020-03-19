package Genetic;

import Individual.Individual;
import Individual.AdSplit;
import ProductAllocation.OrderDistribution;

public class Repair {


    public static boolean repair(Individual individual, OrderDistribution orderDistribution){
        for (int i = 1 ; i < 4 ; i++){
            double penaltyMultiplier = Math.pow(10,i);
            AdSplit.adSplitPlural(individual, penaltyMultiplier);
            Education.improveRoutes(individual, orderDistribution, penaltyMultiplier);
            individual.getFitness(true);
            if (individual.isFeasible()){
                break;
            }
        }

    }


    public static void main(String[] args){
        double i = 10*5;
        System.out.println(Math.pow(10,2));
    }


}
    