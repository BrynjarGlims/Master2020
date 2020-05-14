package Master2020.Run;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.DataFiles.Parameters;
import Master2020.Genetic.*;
import Master2020.Individual.Individual;
import Master2020.Individual.Trip;
import Master2020.MIP.OrderAllocationModel;
import Master2020.PGA.PeriodicIndividual;
import Master2020.PGA.PeriodicPopulation;
import Master2020.Population.OrderDistributionPopulation;
import Master2020.Population.Population;
import Master2020.ProductAllocation.OrderDistribution;
import Master2020.StoringResults.Result;
import Master2020.Testing.IndividualTest;
import Master2020.Visualization.PlotIndividual;
import gurobi.GRBException;

import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

public class GAController {
    public static Data data;
    public static Population population;
    public static PeriodicPopulation periodicPopulation;
    public static OrderDistributionPopulation odp;
    public static OrderDistributionCrossover ODC;
    public static OrderDistribution globalOrderDistribution;
    public static double bestIndividualScore;
    public static HashSet<Individual> repaired;
    public static Individual bestIndividual;
    public static int numberOfIterations;
    public static OrderAllocationModel orderAllocationModel;
    public static double scalingFactorOrderDistribution;
    public static PeriodicIndividual bestPeriodicIndividual;


    public GAController(Data data) throws GRBException {
        initialize(data);
    }

    public void initialize(Data inputData) throws GRBException {
        data = inputData;
        population = new Population(data);
        odp = new OrderDistributionPopulation(data);
        ODC = new OrderDistributionCrossover(data);
        odp.initializeOrderDistributionPopulation(population);
        globalOrderDistribution = odp.getRandomOrderDistribution();
        population.setOrderDistributionPopulation(odp);
        population.initializePopulation(globalOrderDistribution);
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

    private static Individual PIX(Population population){
        // Select parents
        Individual parent1 = TournamentSelection.performSelection(population);
        Individual parent2 = TournamentSelection.performSelection(population);
        while (parent1.equals(parent2)){
            parent2 = TournamentSelection.performSelection(population);
        }
        /*OrderDistribution[] crossoverOD = ODC.crossover(parent1.orderDistribution, parent2.orderDistribution); //these will be the same
        for (OrderDistribution od : crossoverOD) { //todo: EVALUEATE IF THIS IS DECENT
            odp.addOrderDistribution(od);
        }

         */
        return GiantTourCrossover.crossOver(parent1, parent2, globalOrderDistribution); // TODO: 02.04.2020 add to a pool?
    }




    private static void educate(Individual individual){
        if (ThreadLocalRandom.current().nextDouble() < Parameters.educationProbability){
            Education.improveRoutes(individual, individual.orderDistribution);
        }
    }


    private static void setOptimalOrderDistribution(Individual individual){
        if (ThreadLocalRandom.current().nextDouble() < Parameters.ODMIPProbability){
            OrderDistribution optimalOD;
            if (orderAllocationModel.createOptimalOrderDistribution(individual.journeyList) == 2){
                optimalOD = orderAllocationModel.getOrderDistribution();
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
    }

    private static void tripOptimizer(Individual individual){
        for (int p = 0 ; p < individual.numberOfPeriods ; p++){
            for (int vt = 0 ; vt < data.numberOfVehicleTypes ; vt++){
                for (Trip trip : individual.tripList[Individual.getDefaultPeriod(p)][vt]) {
                    if (ThreadLocalRandom.current().nextDouble() < Parameters.tripOptimizerProbability) {
                        TripOptimizer.optimizeTrip(trip, individual.orderDistribution);
                    }
                }
            }
        }
        individual.setGiantTourFromTrips();
    }

    public static void repair(){
        repair(population);
    }

    public static void repair(Population population){
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
        selection(population);
    }


    public static void selection(Population population){

        // Reduce population size

        population.improvedSurvivorSelection();
        if (!Parameters.isPeriodic){
            odp.removeNoneUsedOrderDistributions(population);
        }
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
        Individual bestInfeasibleIndividual = population.returnBestInfeasibleIndividual();
        if (!Parameters.isPeriodic){
            bestFeasibleIndividual.printDetailedFitness();
            bestInfeasibleIndividual.printDetailedFitness();
        }

    }

    public void run() throws IOException, GRBException {
        double time = System.currentTimeMillis();
        System.out.println("Initialize population..");
        initialize(data);

        while ((population.getIterationsWithoutImprovement() < Parameters.maxNumberIterationsWithoutImprovement &&
                numberOfIterations < Parameters.maxNumberOfGenerations)) {
            System.out.println("Start generation: " + numberOfIterations);

            //Find best OD for the distribution
            System.out.println("Assign best OD..");
            findBestOrderDistribution();

            //Generate new population
            for (Individual individual : population.infeasiblePopulation) {
                if (!Master2020.Testing.IndividualTest.testIndividual(individual)) {
                    System.out.println("BEST INDIVIDUAL IS NOT COMPLETE: PRIOR");
                }
            }

            System.out.println("Populate..");
            for (int j = 0; j < Parameters.numberOfIndividualsGeneratedEachGeneration; j++) {
                Individual newIndividual = PIX();
                if (!Master2020.Testing.IndividualTest.testIndividual(newIndividual)) {
                    System.out.println("BEST INDIVIDUAL IS NOT COMPLETE: PIX");
                }
                educate(newIndividual);
                if (!Master2020.Testing.IndividualTest.testIndividual(newIndividual)) {
                    System.out.println("BEST INDIVIDUAL IS NOT COMPLETE: EDUCATE");
                }
                setOptimalOrderDistribution(newIndividual);
                if (!Master2020.Testing.IndividualTest.testIndividual(newIndividual)) {
                    System.out.println("BEST INDIVIDUAL IS NOT COMPLETE: OD OPTIMIZER");
                }
                tripOptimizer(newIndividual);
                if (!Master2020.Testing.IndividualTest.testIndividual(newIndividual)) {
                    System.out.println("BEST INDIVIDUAL IS NOT COMPLETE: TRIP OPTIMIZER");
                }
                population.addChildToPopulation(newIndividual);

            }
            for (Individual individual : population.getTotalPopulation()) {
                IndividualTest.checkIfIndividualIsComplete(individual);
            }

            System.out.println("Repair..");
            repair();

            System.out.println("Selection..");
            selection();

            numberOfIterations++;
        }



        Individual bestIndividual = population.returnBestIndividual();
        if (!Master2020.Testing.IndividualTest.testIndividual(bestIndividual)) {
            System.out.println("BEST INDIVIDUAL IS NOT COMPLETE");
        }
        System.out.println("Individual feasible: " + bestIndividual.isFeasible());
        System.out.println("Fitness: " + bestIndividual.getFitness(false));
        if (Parameters.savePlots) {
            PlotIndividual visualizer = new PlotIndividual(data);
            visualizer.visualize(bestIndividual);
        }
        double runTime = (System.currentTimeMillis() - time)/1000;
        Result res = new Result(population, "GA");
        res.store(runTime, -1);
        orderAllocationModel.terminateEnvironment();
    }
}
