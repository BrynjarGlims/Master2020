package Master2020.Run;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.DataFiles.Parameters;
import Master2020.Genetic.*;
import Master2020.Individual.Individual;
import Master2020.MIP.DataConverter;
import Master2020.MIP.OrderAllocationModel;
import Master2020.PR.DataMIP;
import Master2020.PR.JourneyBasedModel;
import Master2020.Population.Population;
import Master2020.ProductAllocation.OrderDistribution;
import Master2020.Population.OrderDistributionPopulation;
import Master2020.StoringResults.Result;
import Master2020.Testing.IndividualTest;
import Master2020.Visualization.PlotIndividual;
import Master2020.Individual.Trip;
import Master2020.Testing.*;
import gurobi.GRBException;


import gurobi.GRBException;
import scala.xml.PrettyPrinter;

import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

public class App {


    public static Data data;
    public static Population population;
    public static OrderDistributionPopulation odp;
    public static OrderDistributionCrossover ODC;
    public static OrderDistribution firstOD;
    public static double bestIndividualScore;
    public static HashSet<Individual> repaired;
    public static Individual bestIndividual;
    public static int numberOfIterations;
    public static OrderAllocationModel orderAllocationModel;

    public static void initialize() throws GRBException {
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
        orderAllocationModel = new OrderAllocationModel(data);
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
        return GiantTourCrossover.crossOver(parent1, parent2, crossoverOD[0]); // TODO: 02.04.2020 add to a pool?
    }


    public static void educate(Individual individual){
        if (ThreadLocalRandom.current().nextDouble() < Parameters.educationProbability){
            Education.improveRoutes(individual, individual.orderDistribution);
        }
    }


    public static void setOptimalOrderDistribution(Individual individual){
        if (ThreadLocalRandom.current().nextDouble() < Parameters.greedyMIPValue){
            OrderDistribution optimalOD = orderAllocationModel.createOptimalOrderDistribution(individual);
            if (optimalOD.fitness != Double.MAX_VALUE) {  // Distribution found
                if (individual.infeasibilityCost == 0) {
                    individual.setOptimalOrderDistribution(optimalOD, true);
                } else {
                    individual.setOptimalOrderDistribution(optimalOD, false);
                }
                odp.addOrderDistribution(optimalOD);
            }
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
        individual.setGiantTourFromTrips();
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

        // Reduce population size
        population.improvedSurvivorSelection();
        odp.removeNoneUsedOrderDistributions();
        numberOfIterations++;
        bestIndividual = population.returnBestIndividual();
        bestIndividualScore = bestIndividual.getFitness(false);

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


    public static void runMIP(int samples){
        for (int i = 0 ; i < samples ; i++){
            Parameters.randomSeedValue += 1;
            Data data = Master2020.DataFiles.DataReader.loadData();
            DataMIP dataMip = DataConverter.convert(data);
            JourneyBasedModel jbm = new JourneyBasedModel(dataMip);
            jbm.runModel(Master2020.DataFiles.Parameters.symmetry);
        }
        }



    public static void runGA() throws IOException, GRBException {
        System.out.println("Initialize population..");
        initialize();

        while ((population.getIterationsWithoutImprovement() < Parameters.maxNumberIterationsWithoutImprovement &&
                numberOfIterations < Parameters.maxNumberOfGenerations)) {
            System.out.println("Start generation: " + numberOfIterations);

            //Find best OD for the distribution
            System.out.println("Assign best OD..");
            findBestOrderDistribution();

            //Generate new population
            for (Individual individual : population.infeasiblePopulation){
                if (!Master2020.Testing.IndividualTest.testIndividual(individual)){
                    System.out.println("BEST INDIVIDUAL IS NOT COMPLETE: PRIOR");
                }
            }

            System.out.println("Populate..");
            while (population.infeasiblePopulation.size() < Parameters.maximumSubIndividualPopulationSize &&
                    population.feasiblePopulation.size() < Parameters.maximumSubIndividualPopulationSize) {
                Individual newIndividual = PIX();
                if (!Master2020.Testing.IndividualTest.testIndividual(newIndividual)){
                    System.out.println("BEST INDIVIDUAL IS NOT COMPLETE: PIX");
                }
                educate(newIndividual);
                if (!Master2020.Testing.IndividualTest.testIndividual(newIndividual)){
                    System.out.println("BEST INDIVIDUAL IS NOT COMPLETE: EDUCATE");
                }
                setOptimalOrderDistribution(newIndividual);
                if (!Master2020.Testing.IndividualTest.testIndividual(newIndividual)){
                    System.out.println("BEST INDIVIDUAL IS NOT COMPLETE: OD OPTIMIZER");
                }
                tripOptimizer(newIndividual);
                if (!Master2020.Testing.IndividualTest.testIndividual(newIndividual)){
                    System.out.println("BEST INDIVIDUAL IS NOT COMPLETE: TRIP OPTIMIZER");
                }
                population.addChildToPopulation(newIndividual);


            }
            for (Individual individual : population.getTotalPopulation()){
                IndividualTest.checkIfIndividualIsComplete(individual);
            }

            System.out.println("Repair..");
            repair();

            System.out.println("Selection..");
            selection();
        }


        Individual bestIndividual = population.returnBestIndividual();
        if (!Master2020.Testing.IndividualTest.testIndividual(bestIndividual)){
            System.out.println("BEST INDIVIDUAL IS NOT COMPLETE");
        }
        System.out.println("Individual feasible: " + bestIndividual.isFeasible());
        System.out.println("Fitness: " + bestIndividual.getFitness(false));
        if (Parameters.savePlots){
            PlotIndividual visualizer = new PlotIndividual(data);
            visualizer.visualize(bestIndividual);
        }
        Result res = new Result(population);
        res.store();
        orderAllocationModel.terminateEnvironment();

    }



    public static void main(String[] args) throws Exception {
        if (args[0].equals("MIP")){
            runMIP(Parameters.samples);
        }
        else {
            runGA();
        }
    }

}