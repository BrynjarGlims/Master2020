package Genetic;

import Individual.Individual;
import Individual.AdSplit;
import ProductAllocation.OrderDistribution;
import Individual.Label;

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
            individual.updateFitness();
            System.out.println("IS FEASIBLE: " + individual.isFeasible() + " infeasibility cost: " + individual.infeasibilityCost);
            if (individual.isFeasible()){
                return true;
            }
        }
        individual.updateFitness(1.0/Math.pow(10,i));
        return false;
    }


    public static void main(String[] args){
        double i = 10;
        i *= 5;
        System.out.println(i);
        System.out.println(Math.pow(10,2));
    }


}
    