import DataFiles.Data;
import DataFiles.DataReader;
import DataFiles.Order;
import DataFiles.Parameters;
import Genetic.GiantTourCrossover;
import Genetic.OrderDistributionCrossover;
import Individual.Individual;
import MIP.OrderAllocationModel;
import Population.Population;
import ProductAllocation.OrderDistribution;
import Population.OrderDistributionPopulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class main {
    public static void main(String[] args){
        Data data = DataReader.loadData();
        Population population = new Population(data);
        OrderDistributionPopulation odp = new OrderDistributionPopulation(data);
        ArrayList<OrderDistribution> tabuList = new ArrayList<OrderDistribution>();
        GiantTourCrossover GTC = new GiantTourCrossover(data);
        OrderDistributionCrossover ODC = new OrderDistributionCrossover(data);
        OrderDistribution targetOD = odp.getRandomOrderDistribution();

        tabuList.add(targetOD);

        int numberOfIterations = 0;
        while ( population.getIterationsWithoutImprovement() < Parameters.maxNumberIterationsWithoutImprovement &&
                numberOfIterations < Parameters.maxNumberOfIterations){
            if (numberOfIterations == 0){
                population.setOrderDistributionPopulation(odp);
                population.initializePopulation(targetOD);
                odp.initializeOrderDistributionPopulation(population);
            }
            HashMap<Individual, HashMap<OrderDistribution, Double>> crossFitnessScore = odp.getFillingLevelFitnessScoresPlural();

            while (population.getPopulationSize() < Parameters.maxPopulationSize){
                Individual parent1 = population.getRandomIndividual();
                Individual parent2 = population.getRandomIndividual();//todo:base this on crossfitnesscstore
                OrderDistribution[] crossoverOD = ODC.crossover(parent1.orderDistribution, parent2.orderDistribution); //these will be the same
                for (OrderDistribution od : crossoverOD){
                    odp.addOrderDistribution(od);
                }
                Individual newIndividual = GTC.crossOver(parent1, parent2, crossoverOD[0]);

                // TODO: 04.03.2020 Brynjar: Add education
                // TODO: 04.03.2020 Add repair:

                Random rand = new Random();
                if (rand.nextDouble() < Parameters.greedyMIPValue){
                    OrderDistribution optimalOD = OrderAllocationModel.createOptimalOrderDistribution(newIndividual, data);
                }

                population.addChildToPopulation(newIndividual);

            }




            //crossover to obtain a new child
            //for the obtained child:
            //adsplit
            //getIndividualFitnessScore()
            //educate (with probability P_ls)
            //if (child infeasible):
                //repair
            //insert child into relevant subpopulation

            //if (subpopulation.getSize() > maxSize): select survivors:
            //if child.isFeasible();
                //Population.selectFeasibleSurvivors();
            //else
                //population.selectInfeasibleSurvivors();



            //adjust penalty parameters for overtimeInfeasibility, loadInfeasibility and timeWarpInfeasibility
            numberOfIterations++;
        }




    }



}
