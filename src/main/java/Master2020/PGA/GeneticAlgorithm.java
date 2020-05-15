package Master2020.PGA;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.DataFiles.Parameters;
import Master2020.Genetic.*;
import Master2020.Individual.Individual;
import Master2020.Individual.Journey;
import Master2020.Individual.Trip;
import Master2020.Population.PeriodicOrderDistributionPopulation;
import Master2020.Population.Population;
import Master2020.ProductAllocation.OrderDistribution;
import gurobi.GRBException;

import java.util.ArrayList;
import java.util.Collections;
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
    public Individual currentBestIndividual;
    public PenaltyControl penaltyControl;


    //threads
    public CyclicBarrier downstreamGate;
    public CyclicBarrier upstreamGate;
    public boolean run = true;


    public GeneticAlgorithm(Data data){
        this.data = data;
        this.penaltyControl = new PenaltyControl(Parameters.initialTimeWarpPenalty, Parameters.initialOverLoadPenalty, Parameters.frequencyOfPenaltyUpdatesPGA);
    }


    public void initialize(int period, OrderDistribution orderDistribution, CyclicBarrier downstreamGate, CyclicBarrier upstreamGate, Population population) throws GRBException {
        System.out.println("Initialize population..");
        this.population = population;
        this.period = period;
        this.orderDistribution = orderDistribution;
        population.initializePopulation(this.orderDistribution, penaltyControl);
        fitnessForPeriod = Double.MAX_VALUE;
        BiasedFitness.setBiasedFitnessScore(population);
        repaired = new HashSet<>();
        numberOfIterations = 0;
        this.downstreamGate = downstreamGate;
        this.upstreamGate = upstreamGate;
        //System.out.println("Initialization completed");
    }

    public void setPopulation(Population population) {
        this.population = population;
    }

    public Individual PIX(Population population, double timeWarpPenalty, double overLoadPenalty){
        // Select parents
        Individual parent1 = TournamentSelection.performSelection(population);
        Individual parent2 = TournamentSelection.performSelection(population);
        while (parent1.equals(parent2)){
            parent2 = TournamentSelection.performSelection(population);
        }
        return GiantTourCrossover.crossOver(parent1, parent2, this.orderDistribution, penaltyControl); // TODO: 02.04.2020 add to a pool?
    }

    public void educate(Individual individual){
        if (ThreadLocalRandom.current().nextDouble() < Parameters.educationProbability){
            Education.improveRoutes(individual, individual.orderDistribution, penaltyControl.timeWarpPenalty, penaltyControl.overLoadPenalty);
        }
    }

    public ArrayList<Journey>[] getBestJourneysFromIndividuals(){
        ArrayList<Journey>[] tempJourneyList = new ArrayList[data.numberOfVehicleTypes];
        for (int vt = 0; vt < data.numberOfVehicleTypes; vt++){
            tempJourneyList[vt] = new ArrayList<Journey>();
        }
        ArrayList<Individual> potentialIndividuals = new ArrayList<>(population.feasiblePopulation);
        if (Parameters.numberOfIndividualJourneysInMIPPerPeriod > potentialIndividuals.size())
            System.out.println("not enough individuals for this period " + period);
        Collections.sort(potentialIndividuals);
        for (int i = 0; i < Math.min(Parameters.numberOfIndividualJourneysInMIPPerPeriod, potentialIndividuals.size()); i++){
            Individual individual = potentialIndividuals.get(i);
            for (int vt = 0; vt < data.numberOfVehicleTypes; vt++){
                tempJourneyList[vt].addAll(individual.journeyList[0][vt]);
            }
        }
        return tempJourneyList;
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
                if (Repair.repair(infeasibleIndividual, infeasibleIndividual.orderDistribution, penaltyControl)){
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
    }


    public void runGeneration() {

        for (int j = 0; j < Parameters.numberOfIndividualsGeneratedEachGeneration; j++) {
            Individual newIndividual = PIX(population, penaltyControl.timeWarpPenalty, penaltyControl.overLoadPenalty);
            penaltyControl.adjust(newIndividual.hasTimeWarp(), newIndividual.hasOverLoad());
            educate(newIndividual);
            tripOptimizer(newIndividual);
            population.addChildToPopulation(newIndividual);
        }
        repair(population);
        selection(population);

        updateSystemParameters();
    }

    public void updateSystemParameters(){
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
        resetCounters();
        printPopulationStats();
        for (int i = 0 ; i < generations ; i++){
            System.out.println("Start generation " + i);
            if (iterationsWithoutImprovement > Parameters.maxNumberIterationsWithoutImprovement)
                break;
            runGeneration();
        }
        printPopulationStats();
    }

    public void printPopulationStats(){
        System.out.println("Period " + period);
        System.out.println(" Number of feasible individuals: " + population.feasiblePopulation.size());
        System.out.println(" Number of infeasible individuals: " + population.infeasiblePopulation.size());
    }



}
