package Master2020.PGA;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.Parameters;
import Master2020.Genetic.*;
import Master2020.Individual.Individual;
import Master2020.Individual.Trip;
import Master2020.MIP.OrderAllocationModel;
import Master2020.Population.Population;
import Master2020.ProductAllocation.OrderDistribution;
import gurobi.GRBException;

import java.util.HashSet;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;

public class GeneticAlgorithm extends Thread {

    public static Data data;
    public int period;
    public Population population;
    public OrderDistribution orderDistribution;
    public double scalingFactorOrderDistribution;
    public double fitnessForPeriod;
    public HashSet<Individual> repaired;
    public int numberOfIterations;
    public int iterationsWithoutImprovement;
    public OrderAllocationModel orderAllocationModel;
    public boolean isPeriodic = Parameters.isPeriodic;
    public Individual currentBestIndividual;

    //threads
    public CyclicBarrier downstreamGate;
    public CyclicBarrier upstreamGate;
    public boolean run = true;


    public GeneticAlgorithm(Data data){
        this.data = data;
    }


    public void initialize(int period, OrderDistribution orderDistribution, CyclicBarrier downstreamGate, CyclicBarrier upstreamGate, Population population) throws GRBException {
        System.out.println("Initialize population..");
        this.population = population;
        this.period = period;
        this.orderDistribution = orderDistribution;
        population.initializePopulation(this.orderDistribution);
        fitnessForPeriod = Double.MAX_VALUE;
        BiasedFitness.setBiasedFitnessScore(population);
        repaired = new HashSet<>();
        numberOfIterations = 0;
        this.downstreamGate = downstreamGate;
        this.upstreamGate = upstreamGate;
        System.out.println("Initialization completed");
    }

    public void setPopulation(Population population) {
        this.population = population;
    }

    public Individual PIX(Population population){
        // Select parents
        Individual parent1 = TournamentSelection.performSelection(population);
        Individual parent2 = TournamentSelection.performSelection(population);
        while (parent1.equals(parent2)){
            parent2 = TournamentSelection.performSelection(population);
        }
        return GiantTourCrossover.crossOver(parent1, parent2, this.orderDistribution); // TODO: 02.04.2020 add to a pool?
    }

    public void educate(Individual individual){
        if (ThreadLocalRandom.current().nextDouble() < Parameters.educationProbability){
            Education.improveRoutes(individual, individual.orderDistribution);
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

    public void repair(Population population){
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

    public void resetCounters(){
        numberOfIterations = 0;
        iterationsWithoutImprovement = 0;
    }

    public void selection(Population population){

        // Reduce population size

        population.improvedSurvivorSelection();
        currentBestIndividual = population.returnBestIndividual();
        fitnessForPeriod = currentBestIndividual.getFitness(false);


        // Check if it has improved for early termination
        if (fitnessForPeriod <= currentBestIndividual.getFitness(false)){
            iterationsWithoutImprovement += 1;
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


    public void runGeneration() {
        while (population.infeasiblePopulation.size() < Parameters.maximumSubIndividualPopulationSize &&
                population.feasiblePopulation.size() < Parameters.maximumSubIndividualPopulationSize) {
            Individual newIndividual = PIX(population);
            educate(newIndividual);
            tripOptimizer(newIndividual);
            population.addChildToPopulation(newIndividual);
        }
        repair(population);

        selection(population);

        currentBestIndividual = population.returnBestIndividual();
        if (currentBestIndividual.getFitness(false) < fitnessForPeriod && currentBestIndividual.isFeasible()){
            fitnessForPeriod = currentBestIndividual.getFitness(false);
            iterationsWithoutImprovement = 0;
        }
        else{
            iterationsWithoutImprovement += 1;
        }
        this.numberOfIterations += 1;


    }


    @Override
    public void run() {
        while (run){
            try {
                //wait for all threads to be ready
                downstreamGate.await();
                //run generations
                if (run){runGenerations(Parameters.generationsPerOrderDistributionPeriodic);}
                //wait for all periods to finish
                upstreamGate.await();

            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
        }

    }




    public void runGenerations(int generations) {
        for (int i = 0 ; i < generations ; i++){
            if (iterationsWithoutImprovement > Parameters.maxNumberIterationsWithoutImprovement)
                break;
            runGeneration();
        }
    }

}
