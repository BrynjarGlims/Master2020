package Master2020.PGA;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.Parameters;
import Master2020.Genetic.*;
import Master2020.Individual.Individual;
import Master2020.Individual.Trip;
import Master2020.MIP.OrderAllocationModel;
import Master2020.Population.OrderDistributionPopulation;
import Master2020.Population.Population;
import Master2020.ProductAllocation.OrderDistribution;
import Master2020.StoringResults.Result;
import gurobi.GRBException;

import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;

public class GeneticPeriodicAlgorithm extends Thread{
    public static Data data;
    public static Population population;
    public static PeriodicPopulation periodicPopulation;
    public static OrderDistributionPopulation odp;
    public static OrderDistributionCrossover ODC;
    public static OrderDistribution globalOrderDistribution;
    public static HashSet<Individual> repaired;
    public static OrderAllocationModel orderAllocationModel;
    public static PeriodicIndividual bestPeriodicIndividual;
    public static Individual bestIndividual;



    public static double scalingFactorOrderDistribution;
    public static double bestIndividualScore;
    public static int numberOfIterations = 0;
    public static int iterationsWithoutImprovement = 0;

    //threading
    public boolean run;
    private CyclicBarrier downstreamGate;
    private CyclicBarrier upstreamGate;
    private CyclicBarrier masterDownstreamGate;
    private CyclicBarrier masterUpstreamGate;

    public GeneticPeriodicAlgorithm(Data data){
        this.data = data;
    }


    public void initializePeriodic(OrderDistribution od, CyclicBarrier downstreamGate, CyclicBarrier upstreamGate) throws GRBException {
        this.downstreamGate = downstreamGate;
        this.upstreamGate = upstreamGate;
        //todo: find out what to do with the population. Probably remove
        scalingFactorOrderDistribution = Parameters.initialOrderDistributionScale;
        globalOrderDistribution = od;
        periodicPopulation = new PeriodicPopulation(data);
        periodicPopulation.initialize(od);
        ODC = new OrderDistributionCrossover(data);
        odp.initializeOrderDistributionPopulation(periodicPopulation);
        odp.setOrderDistributionScalingFactor(scalingFactorOrderDistribution);
        globalOrderDistribution = odp.getRandomOrderDistribution();
        globalOrderDistribution.setOrderScalingFactor(scalingFactorOrderDistribution);
        periodicPopulation.setOrderDistributionPopulation(odp);
        periodicPopulation.initializePopulation(globalOrderDistribution);
        bestIndividualScore = Double.MAX_VALUE;
        BiasedFitness.setBiasedFitnessScore(periodicPopulation);
        orderAllocationModel = new OrderAllocationModel(data);
        repaired = new HashSet<>();
        numberOfIterations = 0;
    }


    public void updateOrderDistribution(OrderDistribution orderDistribution){
        this.globalOrderDistribution = orderDistribution;
        //todo: reset the entire algorithm!
        numberOfIterations = 0;
    }

    public static OrderDistribution getGlobalOrderDistribution() {
        return globalOrderDistribution;
    }

    public static Individual PIX(Population population){
        // Select parents
        Individual parent1 = TournamentSelection.performSelection(population);
        Individual parent2 = TournamentSelection.performSelection(population);
        while (parent1.equals(parent2)){
            parent2 = TournamentSelection.performSelection(population);
        }

        return GiantTourCrossover.crossOver(parent1, parent2, globalOrderDistribution);
    }


    public static void educate(Individual individual){
        if (ThreadLocalRandom.current().nextDouble() < Parameters.educationProbability){
            Education.improveRoutes(individual, individual.orderDistribution);
        }
    }



    public static void tripOptimizer(Individual individual){
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









    public static void runIteration() throws IOException, GRBException {
        int samples = Parameters.samples;
        Parameters.isPeriodic = true;
        double time = System.currentTimeMillis();
        for (int i = 0; i < samples; i++) {
            System.out.println("Initialize population..");
            System.out.println("Initialization completed");

            while (periodicPopulation.getIterationsWithoutImprovement() < Parameters.maxNumberIterationsWithoutImprovement &&
                    numberOfIterations < Parameters.maxNumberOfGenerations) {
                System.out.println("Start generation: " + numberOfIterations);

                //System.out.println("Populate..");
                for (int p = 0; p < data.numberOfPeriods; p++) {
                    population = periodicPopulation.populations[p];

                }
                //System.out.println("Iteration ended");

                PeriodicIndividual newPeriodicIndividual = generateGreedyPeriodicIndividual();
                periodicPopulation.addPeriodicIndividual(newPeriodicIndividual);
                newPeriodicIndividual.printDetailedInformation();
                PeriodicIndividual bestPeriodicIndividual = periodicPopulation.returnBestIndividual();



                if (numberOfIterations % Parameters.generationsOfOrderDistributions == 0 ||
                        numberOfIterations == Parameters.maxNumberIterationsWithoutImprovement-1) {
                    createNewOptimalOrderDistribution(bestPeriodicIndividual);
                }
                updateOrderDistributionScalingParameter();

                System.out.println("Number of ORDERDISTRIBUTIONS: " + odp.setOfOrderDistributions.size());
                bestPeriodicIndividual = periodicPopulation.returnBestIndividual();
                //bestPeriodicIndividual.printDetailedInformation();
                numberOfIterations += 1;
            }



            Result res = new Result(bestIndividual, "PGA" , bestPeriodicIndividual.isFeasible(), false);
            double runTime = (System.currentTimeMillis() - time)/1000;
            res.store(runTime, -1);
            orderAllocationModel.terminateEnvironment();
            Parameters.randomSeedValue += 1;
        }
    }

    private static void createNewOptimalOrderDistribution(PeriodicIndividual bestPeriodicIndividual){
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


    private static PeriodicIndividual generateGreedyPeriodicIndividual(){
        PeriodicIndividual newPeriodicIndividual = new PeriodicIndividual(data);
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




    private static void updateOrderDistributionScalingParameter() {
        if (numberOfIterations % Parameters.numberOfGenerationsBetweenODScaling == 0 && numberOfIterations > Parameters.numberOfGenerationBeforeODScalingStarts) {
            scalingFactorOrderDistribution = (scalingFactorOrderDistribution < 1) ? Parameters.incrementPerOrderDistributionScaling + scalingFactorOrderDistribution : 1;
            globalOrderDistribution.setOrderScalingFactor(scalingFactorOrderDistribution);
            odp.setOrderDistributionScalingFactor(scalingFactorOrderDistribution);
            periodicPopulation.reassignPeriodicIndividuals();
            System.out.println("############# CURRENT ORDER DISTRIBUTION SCALING IS " + scalingFactorOrderDistribution + "################");
        }
    }

    private static PeriodicIndividual generatePeriodicIndividual(){
        PeriodicIndividual newPeriodicIndividual = new PeriodicIndividual(data);
        newPeriodicIndividual.setOrderDistribution(globalOrderDistribution);
        for (int p = 0; p < data.numberOfPeriods; p++){
            Individual individual = TournamentSelection.performSelection(periodicPopulation.populations[p]);
            newPeriodicIndividual.setPeriodicIndividual(individual, p);
        }
        System.out.println("Fitness: " + newPeriodicIndividual.getFitness());
        return newPeriodicIndividual;
    }


    public PGASolution storeSolution(){
        bestPeriodicIndividual = periodicPopulation.returnBestIndividual();
        PGASolution pgaSolution = bestPeriodicIndividual.createPGASolution();
        pgaSolution.individual.updateFitness();
        pgaSolution.individual.printDetailedFitness();
        return pgaSolution;
    }


    public static void main(String[] args) throws Exception {


        //Parameters.numberOfCustomers = Integer.parseInt(args[1]);
        /*
        if (args[0].equals("AFM"))
            runMIPAFM(Parameters.samples);
        else if (args[0].equals("PFM"))
            runMIPPFM(Parameters.samples);
        else if (args[0].equals("JBM"))
            runMIPJBM(Parameters.samples);
        else if (args[0].equals("GA"))
            runGA(Parameters.samples);
        else if (args[0].equals("PGA")) {
            Parameters.isPeriodic = true;
            runPeriodicGA(Parameters.samples);
        }

         */


    }

}
