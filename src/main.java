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
import Testing.IndividualTest;
import Visualization.PlotIndividual;
import Individual.Trip;
import Testing.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

public class main {


    public static Data data;
    public static Population population;
    public static OrderDistributionPopulation odp;
    public static OrderDistributionCrossover ODC;
    public static OrderDistribution firstOD;
    public static double bestIndividualScore;
    public static HashSet<Individual> repaired;
    public static Individual bestIndividual;
    public static int numberOfIterations;

    public static void initialize(){
        data = DataReader.loadData();
        population = new Population(data);
        odp = new OrderDistributionPopulation(data);
        ODC = new OrderDistributionCrossover(data);
        odp.initializeOrderDistributionPopulation(population);
        firstOD = odp.getRandomOrderDistribution();
        population.setOrderDistributionPopulation(odp);
        population.initializePopulation(firstOD);
        bestIndividualScore = Double.MAX_VALUE;
        BiasedFitness.setBiasedFitnessScore(population);
        repaired = new HashSet<>();
        numberOfIterations = 0;
    }

    private static void findBestOrderDistribution(){
        //Find best OD for the distribution
        odp.calculateFillingLevelFitnessScoresPlural();
        for (Individual individual : population.getTotalPopulation()){
            individual.testNewOrderDistribution(odp.getBestOrderDistribution(individual)); // // TODO: 24/03/2020 Rejects converting from feasible to infeasible
        }
    }

    public static Individual PIX(){
        // Select parents
        Individual parent1 = TournamentSelection.performSelection(population);
        Individual parent2 = TournamentSelection.performSelection(population);
        while (parent1.equals(parent2)){
            parent2 = TournamentSelection.performSelection(population);
        }
        OrderDistribution[] crossoverOD = ODC.crossover(parent1.orderDistribution, parent2.orderDistribution); //these will be the same
        for (OrderDistribution od : crossoverOD) { //todo: EVALUEATE IF THIS IS DECENT
            odp.addOrderDistribution(od);
        }
        return GiantTourCrossover.crossOver(parent1, parent2, crossoverOD[0]);
    }


    public static void educate(Individual individual){
        if (ThreadLocalRandom.current().nextDouble() < Parameters.educationProbability){
            Education.improveRoutes(individual, individual.orderDistribution);
        }
    }


    public static void setOptimalOrderDistribution(Individual individual){
        if (ThreadLocalRandom.current().nextDouble() < Parameters.greedyMIPValue){
            System.out.println("------------------Optimal orderdis is gathered---------------------");
            //System.out.println("--------------------");
            //System.out.println("Current fintness: " + newIndividual.getBiasedFitness());
            OrderDistribution optimalOD = OrderAllocationModel.createOptimalOrderDistribution(individual, data);
            if (individual.infeasibilityCost == 0){
                individual.setOptimalOrderDistribution(optimalOD, true);
            }
            else{
                individual.setOptimalOrderDistribution(optimalOD, false);
            }

            odp.addOrderDistribution(optimalOD);  // todo: do not remove adsplit
            //System.out.println("New fitness: " + newIndividual.getBiasedFitness());
            // TODO: 04.03.2020 Implement safe trap in case no solution is found in gurobi
        }
    }

    public static void tripOptimizer(Individual individual){
        for (int p = 0 ; p < data.numberOfPeriods ; p++){
            for (int vt = 0 ; vt < data.numberOfVehicleTypes ; vt++){
                for (Trip trip : individual.tripList[p][vt]) {
                    if (ThreadLocalRandom.current().nextDouble() < Parameters.tripOptimizerProbability) {
                        TripOptimizer.optimizeTrip(trip, individual.orderDistribution);
                    }
                }
            }
        }
    }

    public static void repair(){
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
    }


    public static void selection(){
        // Calculate diversity
        BiasedFitness.setBiasedFitnessScore(population);

        // Reduce population size
        population.survivorSelection();
        odp.removeNoneUsedOrderDistributions();

        // TODO: 04.03.2020 Implement adjust penalty parameters for overtimeInfeasibility, loadInfeasibility and timeWarpInfeasibility
        numberOfIterations++;
        bestIndividual = population.returnBestIndividual();

        // Check if it has improved for early termination
        if (bestIndividualScore == bestIndividual.getFitness(false)){
            population.setIterationsWithoutImprovement(population.getIterationsWithoutImprovement()+1);
        }
        else{
            population.setIterationsWithoutImprovement(0);
        }

        Individual bestFeasibleIndividual = population.returnBestIndividual();
        bestFeasibleIndividual.printDetailedFitness();
    }


    public static void main(String[] args) throws Exception {
        initialize();
        while ( (population.getIterationsWithoutImprovement() < Parameters.maxNumberIterationsWithoutImprovement &&
                numberOfIterations < Parameters.maxNumberOfGenerations) || bestIndividualScore < 3500 ){
            System.out.println("Start generation: " + numberOfIterations);

            //Find best OD for the distribution
            findBestOrderDistribution();

            //Generate new population
            while (population.getPopulationSize() < Parameters.maximumSubIndividualPopulationSize){
                Individual newIndividual = PIX();

                educate(newIndividual);

                setOptimalOrderDistribution(newIndividual);

                tripOptimizer(newIndividual);

                population.addChildToPopulation(newIndividual);

            }

            repair();

            selection();

            // TODO: 19.03.2020 all individuals must have updated fitness before selection is done, because penalties for infeasible individuals
            // TODO: 19.03.2020  which did not complete repair have values in label which is scaled with penalties, but in getFitness calculation
            // TODO: 19.03.2020 the values have been scaled down manually
        }

        Individual bestIndividual = population.returnBestIndividual();
        System.out.println("Individual feasible: " + bestIndividual.isFeasible());
        System.out.println("Fitness: " + bestIndividual.getFitness(false));
        PlotIndividual visualizer = new PlotIndividual(data);
        visualizer.visualize(bestIndividual);
        Result res = new Result(population);
        res.store();
    }


}
