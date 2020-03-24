import DataFiles.Data;
import DataFiles.DataReader;
import DataFiles.Parameters;
import Genetic.*;
import Individual.Individual;
import MIP.OrderAllocationModel;
import Population.Population;
import ProductAllocation.OrderDistribution;
import Population.OrderDistributionPopulation;
import StoringResults.Result;
import Visualization.PlotIndividual;
import Genetic.BiasedFitness;

import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

public class main {
    public static void main(String[] args) throws IOException {
        Data data = DataReader.loadData();
        Population population = new Population(data);
        OrderDistributionPopulation odp = new OrderDistributionPopulation(data);
        OrderDistributionCrossover ODC = new OrderDistributionCrossover(data);
        odp.initializeOrderDistributionPopulation(population);
        OrderDistribution firstOD = odp.getRandomOrderDistribution();
        population.setOrderDistributionPopulation(odp);
        population.initializePopulation(firstOD);


        HashSet<Individual> repaired = new HashSet<>();
        int numberOfIterations = 0;
        while ( population.getIterationsWithoutImprovement() < Parameters.maxNumberIterationsWithoutImprovement &&
                numberOfIterations < Parameters.maxNumberOfGenerations){
            population.setSurvivorsForNextGeneration();
            System.out.println("Start generation: " + numberOfIterations);

            //Find best OD for the distribution
            odp.calculateFillingLevelFitnessScoresPlural();
            for (Individual individual : population.infeasiblePopulation){ // TODO: 16/03/2020 Add for feasible population as well
                individual.testNewOrderDistribution(odp.getBestOrderDistribution(individual)); // TODO: 05.03.2020 Uncomment
            }

            //Generate new population
            while (population.getPopulationSize() < Parameters.maximumSubIndividualPopulationSize){
                Individual parent1 = population.getRandomIndividual(); // TODO: 16.03.2020 get parents based on tournament
                Individual parent2 = population.getRandomIndividual();  //todo:base this on crossfitnesscstore
                OrderDistribution[] crossoverOD = ODC.crossover(parent1.orderDistribution, parent2.orderDistribution); //these will be the same
                for (OrderDistribution od : crossoverOD){
                    odp.addOrderDistribution(od);
                }
                Individual newIndividual = GiantTourCrossover.crossOver(parent1, parent2, crossoverOD[0]);


                Education.improveRoutes(newIndividual, newIndividual.orderDistribution);


                // TODO: 04.03.2020 Add repair:

                if (ThreadLocalRandom.current().nextDouble() < Parameters.greedyMIPValue){
                    System.out.println("------------------Optimal orderdis is gathered---------------------");
                    //System.out.println("--------------------");
                    //System.out.println("Current fintness: " + newIndividual.getBiasedFitness());
                    OrderDistribution optimalOD = OrderAllocationModel.createOptimalOrderDistribution(newIndividual, data);
                    if (newIndividual.infeasibilityCost == 0){
                        newIndividual.setOptimalOrderDistribution(optimalOD, true);
                    }
                    else{
                        newIndividual.setOptimalOrderDistribution(optimalOD, false);
                    }

                    odp.addOrderDistribution(optimalOD);  // todo: do not remove adsplit
                    //System.out.println("New fitness: " + newIndividual.getBiasedFitness());
                    // TODO: 04.03.2020 Implement safe trap in case no solution is found in gurobi
                }
                population.addChildToPopulation(newIndividual);
            }

            System.out.println("Number of feasible pop: " + population.feasiblePopulation.size());
            System.out.println("Number of infeasible pop: " + population.infeasiblePopulation.size());

            repaired.clear();
            for (Individual infeasibleIndividual : population.infeasiblePopulation){
                if (ThreadLocalRandom.current().nextDouble() < Parameters.repairProbability){
                    if (Repair.repair(infeasibleIndividual, infeasibleIndividual.orderDistribution)){
                        repaired.add(infeasibleIndividual);

                    }
                }
            }
            population.infeasiblePopulation.removeAll(repaired);
            population.feasiblePopulation.addAll(repaired);

            // TODO: 19.03.2020 all individuals must have updated fitness before selection is done, because penalties for infeasible individuals
            // TODO: 19.03.2020  which did not complete repair have values in label which is scaled with penalties, but in getFitness calculation
            // TODO: 19.03.2020 the values have been scaled down manually
            //reduce size of both populations

            BiasedFitness.setBiasedFitnessScore(population);
            population.survivorSelection();
            odp.removeNoneUsedOrderDistributions();


            // TODO: 04.03.2020 Implement adjust penalty parameters for overtimeInfeasibility, loadInfeasibility and timeWarpInfeasibility
            numberOfIterations++;
            Individual bestIndividual = population.returnBestIndividual();
            bestIndividual.printDetailedFitness();
            Individual bestFeasibleIndividual = population.returnBestFeasibleIndividual();
            Individual bestInfeasibleIndividual = population.returnBestInfeasibleIndividual();
            if(bestIndividual.isFeasible()){
                System.out.println("Best feasible individual: " + bestFeasibleIndividual.getFitness(false));
            }
            System.out.println("Best infeasible individual: " + bestInfeasibleIndividual.getFitness(false));

        }
        numberOfIterations++;
        Individual bestIndividual = population.returnBestIndividual();
        System.out.println("Individual feasible: " + bestIndividual.isFeasible());
        System.out.println("Fitness: " + bestIndividual.getFitness(false));
        PlotIndividual visualizer = new PlotIndividual(data);
        visualizer.visualize(bestIndividual);
        Result res = new Result(population);
        res.store();
    }
}
