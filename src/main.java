import DataFiles.Data;
import DataFiles.DataReader;
import DataFiles.Parameters;
import Genetic.GiantTourCrossover;
import Genetic.OrderDistributionCrossover;
import Individual.Individual;
import MIP.OrderAllocationModel;
import Population.Population;
import ProductAllocation.OrderDistribution;
import Population.OrderDistributionPopulation;

import java.util.Random;

public class main {
    public static void main(String[] args){
        Data data = DataReader.loadData();
        Population population = new Population(data);
        OrderDistributionPopulation odp = new OrderDistributionPopulation(data);
        GiantTourCrossover GTC = new GiantTourCrossover(data);
        OrderDistributionCrossover ODC = new OrderDistributionCrossover(data);
        odp.initializeOrderDistributionPopulation(population);
        OrderDistribution firstOD = odp.getRandomOrderDistribution();
        population.setOrderDistributionPopulation(odp);
        population.initializePopulation(firstOD);

        int numberOfIterations = 0;
        while ( population.getIterationsWithoutImprovement() < Parameters.maxNumberIterationsWithoutImprovement &&
                numberOfIterations < Parameters.maxNumberOfIterations){
            System.out.println("Start generation: " + numberOfIterations);
            //Find best OD for the distribution
            odp.calculateFillingLevelFitnessScoresPlural();
            for (Individual individual : population.infeasiblePopulation){
                individual.testNewOrderDistribution(odp.getBestOrderDistribution(individual));
            }

            //Generate new population
            while (population.getPopulationSize() < Parameters.maximumSubPopulationSize){
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
                    newIndividual.setOptimalOrderDistribution(optimalOD);
                    odp.addOrderDistribution(optimalOD);
                    // TODO: 04.03.2020 Implement safe trap in case no solution is found in gurobi
                }
                population.addChildToPopulation(newIndividual);

                //remember
                newIndividual.testNewOrderDistribution(odp.getRandomOrderDistribution());

            }

            //reduce size of both populations
            population.reduceSizeToMin();
            odp.removeNoneUsedOrderDistributions();

            // TODO: 04.03.2020 Implement adjust penalty parameters for overtimeInfeasibility, loadInfeasibility and timeWarpInfeasibility

            numberOfIterations++;
        }
        Individual bestIndividual = population.returnBestIndividual();
        System.out.println(bestIndividual.toString());
        System.out.println("Individual infeasible: " + bestIndividual.isFeasible());
        System.out.println("Fitness: " + bestIndividual.getFitness(false));
    }
}
