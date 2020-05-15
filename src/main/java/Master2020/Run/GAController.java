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
import Master2020.StoringResults.SolutionStorer;
import Master2020.Testing.IndividualTest;
import Master2020.Visualization.PlotIndividual;
import gurobi.GRBException;

import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

public class GAController {


    public Data data;
    public Population population;
    public PeriodicPopulation periodicPopulation;
    public OrderDistributionPopulation odp;
    public OrderDistributionCrossover ODC;
    public OrderDistribution globalOrderDistribution;
    public double bestIndividualScore;
    public HashSet<Individual> repaired;
    public Individual bestIndividual;
    public int numberOfIterations;
    public OrderAllocationModel orderAllocationModel;
    public double scalingFactorOrderDistribution;
    public PeriodicIndividual bestPeriodicIndividual;
    public PenaltyControl penaltyControl;

    public String fileName;
    public String modelName = "GA";
    public double time;

    public GAController() throws GRBException {
        initialize();
    }


    public void initialize() throws GRBException {
        data = DataReader.loadData();
        population = new Population(data);
        penaltyControl = new PenaltyControl(Parameters.initialTimeWarpPenalty, Parameters.initialOverLoadPenalty);
        odp = new OrderDistributionPopulation(data);
        ODC = new OrderDistributionCrossover(data);
        odp.initializeOrderDistributionPopulation(population);
        globalOrderDistribution = odp.getRandomOrderDistribution();
        population.setOrderDistributionPopulation(odp);
        population.initializePopulation(globalOrderDistribution, penaltyControl);
        bestIndividualScore = Double.MAX_VALUE;
        fileName = SolutionStorer.getFolderName(this.modelName);
        BiasedFitness.setBiasedFitnessScore(population);
        orderAllocationModel = new OrderAllocationModel(data);
        repaired = new HashSet<>();
        numberOfIterations = 0;
    }


    private void findBestOrderDistribution(){
        //Find best OD for the distribution
        odp.calculateFillingLevelFitnessScoresPlural();
        for (Individual individual : population.getTotalPopulation()){
            individual.testNewOrderDistribution(odp.getBestOrderDistribution(individual)); // // TODO: 24/03/2020 Rejects converting from feasible to infeasible
        }
    }


    public Individual PIX(){
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
        return GiantTourCrossover.crossOver(parent1, parent2, crossoverOD[0], penaltyControl); // TODO: 02.04.2020 add to a pool?
    }


    public void educate(Individual individual, PenaltyControl penaltyControl){
        if (ThreadLocalRandom.current().nextDouble() < Parameters.educationProbability){
            Education.improveRoutes(individual, individual.orderDistribution, penaltyControl.timeWarpPenalty, penaltyControl.overLoadPenalty);
        }
    }


    public void setOptimalOrderDistribution(Individual individual){
        if (ThreadLocalRandom.current().nextDouble() < Parameters.ODMIPProbability){
            OrderDistribution optimalOD;
            if (orderAllocationModel.createOptimalOrderDistribution(individual.journeyList) == 2){
                optimalOD = orderAllocationModel.getOrderDistribution();
                if (optimalOD.fitness != Double.MAX_VALUE) {  // Distribution found
                    individual.setOptimalOrderDistribution(optimalOD);
                    odp.addOrderDistribution(optimalOD);
                }
            }

        }
    }

    public void tripOptimizer(Individual individual){
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

    public void repair(){
        repair(population);
    }

    public void repair(Population population){
        repaired.clear();
        for (Individual infeasibleIndividual : population.infeasiblePopulation){
            if (ThreadLocalRandom.current().nextDouble() < Parameters.repairProbability){
                if (Repair.repair(infeasibleIndividual, infeasibleIndividual.orderDistribution, penaltyControl)){
                    repaired.add(infeasibleIndividual);
                }
            }
        }
        population.infeasiblePopulation.removeAll(repaired);
        population.feasiblePopulation.addAll(repaired);
    }

    public void selection(){
        selection(population);
    }


    public void selection(Population population){

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
            //bestFeasibleIndividual.printDetailedFitness();
        }

    }



    public void runGA() throws IOException, GRBException {
        double time = System.currentTimeMillis();
        System.out.println("Initialize population..");
        initialize();

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
                penaltyControl.adjust(newIndividual.hasTimeWarp(), newIndividual.hasOverLoad());
                //System.out.println("Time warp: " + newIndividual.timeWarpCost + " overLoad: " + newIndividual.overLoadCost);
                if (!Master2020.Testing.IndividualTest.testIndividual(newIndividual)) {
                    System.out.println("BEST INDIVIDUAL IS NOT COMPLETE: PIX");
                }
                educate(newIndividual, penaltyControl);
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
            /*
            System.out.println("Feas pop: " + population.feasiblePopulation.size());
            System.out.println("Infeas pop: " + population.infeasiblePopulation.size());
            */
            selection();
            /*
            System.out.println("Feas pop after: " + population.feasiblePopulation.size());
            System.out.println("Infeas pop after: " + population.infeasiblePopulation.size());

             */

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
        Result res = new Result(population, "GA", fileName);
        res.store(runTime, -1);
        orderAllocationModel.terminateEnvironment();
    }



    private void createNewOptimalOrderDistribution(PeriodicIndividual bestPeriodicIndividual){
        System.out.println("Perform update of volumes");
        if (orderAllocationModel.createOptimalOrderDistribution(bestPeriodicIndividual.getJourneys(), scalingFactorOrderDistribution) == 2){
            globalOrderDistribution = orderAllocationModel.getOrderDistribution();
            odp.setOfOrderDistributions.add(globalOrderDistribution);
            odp.addOrderDistribution(orderAllocationModel.getOrderDistribution());
            periodicPopulation.setOrderDistribution(globalOrderDistribution);
            bestPeriodicIndividual.setOrderDistribution(globalOrderDistribution);
            periodicPopulation.allocateIndividual(bestPeriodicIndividual);
            periodicPopulation.reassignPeriodicIndividuals();
            System.out.println("################################## Number of fesasible individuals:" + periodicPopulation.periodicFeasibleIndividualPopulation.size());
            System.out.println("-----------------Optimal OD found!");
        } else{
            System.out.println("----------------No optimal OD found...");
        }
    }


    private PeriodicIndividual generateGreedyPeriodicIndividual(){
        PeriodicIndividual newPeriodicIndividual = new PeriodicIndividual(data, null);
        newPeriodicIndividual.setOrderDistribution(globalOrderDistribution);
        for (int p = 0; p < data.numberOfPeriods; p++){
            Individual individual = periodicPopulation.populations[p].returnBestIndividual();
            if (!individual.orderDistribution.equals(globalOrderDistribution)){
                System.out.println("------ Trying to combine individuals with diffferent ODS ------- " + individual.hashCode());
            }
            individual.updateFitness();
            if (!individual.isFeasible()){
                System.out.println("------- The added individual is not feasible -------- " + individual.hashCode());
            }
            newPeriodicIndividual.setPeriodicIndividual(individual, p);
        }
        System.out.println("Fitness: " + newPeriodicIndividual.getFitness());
        return newPeriodicIndividual;
    }
}
