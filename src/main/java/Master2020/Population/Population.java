package Master2020.Population;
import Master2020.DataFiles.*;
import Master2020.Genetic.BiasedFitness;
import Master2020.Genetic.OrderDistributionCrossover;
import Master2020.Genetic.TournamentSelection;
import Master2020.Individual.Individual;
import Master2020.Individual.AdSplit;
import Master2020.PGA.PeriodicIndividual;
import Master2020.MIP.OrderAllocationModel;
import Master2020.PGA.PeriodicPopulation;
import Master2020.ProductAllocation.OrderDistribution;


import java.util.ArrayList;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;
import java.util.*;


public class Population {
    public Data data;
    public Set<Individual> feasiblePopulation;
    public Set<Individual> infeasiblePopulation;
    public OrderDistributionPopulation orderDistributionPopulation;

    int iterationsWithoutImprovement = 0;

    boolean isPeriodic;
    int actualPeriod;
    int numberOfPeriods;

    public static PeriodicPopulation periodicPopulation;
    public static OrderDistributionPopulation odp;
    public static OrderDistributionCrossover ODC;
    public static OrderDistribution globalOrderDistribution;
    public static double bestIndividualScore;
    public static HashSet<Individual> repaired;
    public static Individual bestIndividual;
    public static int numberOfIterations;
    public static OrderAllocationModel orderAllocationModel;
    public static PeriodicIndividual bestPeriodicIndividual;



    public Population(Data data, int actualPeriod) {
        this.data = data;
        this.feasiblePopulation = new HashSet<Individual>();
        this.infeasiblePopulation = new HashSet<Individual>();
        this.isPeriodic = (actualPeriod != -1);
        this.numberOfPeriods = (isPeriodic) ? 1 : data.numberOfPeriods;
        this.actualPeriod = actualPeriod;
    }

    public Population(Data data){
        this(data, -1);   //used if not periodic
    }




    public void updateOrderDistributionsOfAllIndividuals(OrderDistribution orderDistribution){
        for (Individual individual : feasiblePopulation){
            individual.orderDistribution =  orderDistribution;
        }
        for (Individual individual : infeasiblePopulation){
            individual.orderDistribution =  orderDistribution;
        }
    }


    public void initializePopulation (OrderDistribution od) {
        for (int i = 0; i < Parameters.initialPopulationSize; i++) {
            Individual individual = new Individual(this.data, this, isPeriodic, actualPeriod);
            individual.initializeIndividual(od);
            AdSplit.adSplitPlural(individual);
            individual.updateFitness();
            if (individual.isFeasible()) {
                feasiblePopulation.add(individual);
            }
            else {
                infeasiblePopulation.add(individual);
            }
        }
    }


    private static double getFitnessDifference(Individual i1, Individual i2) {
        return (Math.abs(i1.getFitness(false) - i2.getFitness(false)));
    }

    public void addChildToPopulation(Individual individual){
        if (individual.isFeasible()){
            feasiblePopulation.add(individual);
        }
        else{
            infeasiblePopulation.add(individual);
        }

    }

    public Individual returnBestIndividual(){
        Individual bestIndividual = null;
        double fitnessScore = Double.MAX_VALUE;
        for (Individual individual : feasiblePopulation){
            if (individual.getFitness(false) < fitnessScore){
                bestIndividual = individual;
                fitnessScore = individual.getFitness(false);
            }
        }
        if (bestIndividual != null){
            return bestIndividual;
        }
        for (Individual individual : infeasiblePopulation){
            if (individual.getFitness(false) < fitnessScore){
                bestIndividual = individual;
                fitnessScore = individual.getFitness(false);
            }
        }
        return bestIndividual;
    }

    public void reassignIndividualsInPopulations(){
        Set<Individual> tempFeasiblePopulation = new HashSet<>();
        Set<Individual> tempInfeasiblePopulation = new HashSet<>();
        for (Individual individual : feasiblePopulation){
            individual.updateFitness();  //todo: may be deleted.
            if (individual.isFeasible()){
                tempFeasiblePopulation.add(individual);
            }
            else{
                tempInfeasiblePopulation.add(individual);
            }
        }
        for (Individual individual : infeasiblePopulation){
            individual.updateFitness();  //todo: may be deleted.
            if (individual.isFeasible()){
                tempFeasiblePopulation.add(individual);
            }
            else{
                tempInfeasiblePopulation.add(individual);
            }
        }
        feasiblePopulation = tempFeasiblePopulation;
        infeasiblePopulation = tempInfeasiblePopulation;
    }


    public Individual returnBestFeasibleIndividual(){
        Individual bestIndividual = null;
        double fitnessScore = Double.MAX_VALUE;
        for (Individual individual : feasiblePopulation){
            if (individual.getFitness(false) < fitnessScore){
                bestIndividual = individual;
                fitnessScore = individual.getFitness(false);
            }
        }
        return bestIndividual;

    }

    public Individual returnBestInfeasibleIndividual(){
        Individual bestIndividual = null;
        double fitnessScore = Double.MAX_VALUE;

        for (Individual individual : infeasiblePopulation){
            if (individual.getFitness(false) < fitnessScore){
                bestIndividual = individual;
                fitnessScore = individual.getFitness(false);
            }
        }
        return bestIndividual;
    }



    public HashSet<Individual> getTotalPopulation(){
        HashSet<Individual> populationSet = new HashSet<Individual>(feasiblePopulation);
        populationSet.addAll(infeasiblePopulation);
        return populationSet;
    }

    public Individual getRandomIndividual(){
        int populationSize = infeasiblePopulation.size() + feasiblePopulation.size();
        int randomIndex = ThreadLocalRandom.current().nextInt(0,populationSize);
        int currentIndex = 0;
        for (Individual individual : feasiblePopulation){
            if (randomIndex == currentIndex) {
                return individual;
            }
            currentIndex++;
        }
        for (Individual individual : infeasiblePopulation){
            if (randomIndex == currentIndex) {
                return individual;
            }
            currentIndex++;
        }
        return null;
    }




    public void setIterationsWithoutImprovement(int iterations){
        this.iterationsWithoutImprovement = iterations;
    }
    public int getIterationsWithoutImprovement(){
        return this.iterationsWithoutImprovement;
    }

    public int getPopulationSize(){
        return feasiblePopulation.size() + infeasiblePopulation.size();
    }

    public void survivorSelection(){
        this.reduceFeasiblePopulation();
        this.reduceInfeasiblePopulation();
    }

    public void improvedSurvivorSelection(){
        int feasibleIndividualsToRemove = this.feasiblePopulation.size() - Parameters.minimumSubIndividualPopulationSize;
        int infeasibleIndividualsToRemove = this.infeasiblePopulation.size() - Parameters.minimumSubIndividualPopulationSize;
        for (int i = 0; i < feasibleIndividualsToRemove; i++){
            if ( i % Parameters.diversityCalculationInterval == 0) {
                BiasedFitness.setBiasedFitnessScoreForFeasibleIndividuals(this);
            }
            reduceFeasiblePopulationByOne();
        }

        for (int i = 0; i < infeasibleIndividualsToRemove; i++){
            if ( i % Parameters.diversityCalculationInterval == 0) {
                BiasedFitness.setBiasedFitnessScoreForInfeasibleIndividuals(this);
            }
            reduceInfeasiblePopulationByOne();
        }
    }

    private void reduceFeasiblePopulationByOne(){
        ArrayList<Individual> worstIndividuals = new ArrayList<Individual>(feasiblePopulation);
        Collections.sort(worstIndividuals);
        Individual worstIndividual = worstIndividuals.get(worstIndividuals.size()-1) ;
        feasiblePopulation.remove(worstIndividual);
    }

    private void reduceInfeasiblePopulationByOne(){
        ArrayList<Individual> worstIndividuals = new ArrayList<Individual>(infeasiblePopulation);
        Collections.sort(worstIndividuals);
        Individual worstIndividual = worstIndividuals.get(worstIndividuals.size()-1) ;
        infeasiblePopulation.remove(worstIndividual);
    }


    private void reduceFeasiblePopulation(){
        int numberOfIndividualsToRemove = feasiblePopulation.size() - Parameters.minimumSubIndividualPopulationSize;
        ArrayList<Individual> worstIndividuals = new ArrayList<Individual>();
        for (Individual individual : feasiblePopulation){
            if ( individual.isSurvivor){
                continue;
            }
            worstIndividuals.add(individual);
            if(worstIndividuals.size() < numberOfIndividualsToRemove){
                Collections.sort(worstIndividuals);
            }
            else{
                Collections.sort(worstIndividuals);
                worstIndividuals.remove(worstIndividuals.size()-1);
            }
        }
        this.feasiblePopulation.removeAll(worstIndividuals);
    }

    private void reduceInfeasiblePopulation(){
        int numberOfIndividualsToRemove = infeasiblePopulation.size() - Parameters.minimumSubIndividualPopulationSize;
        if (numberOfIndividualsToRemove < 0)
            return;
        ArrayList<Individual> worstIndividuals = new ArrayList<Individual>();
        for (Individual individual : infeasiblePopulation){
            if ( individual.isSurvivor){
                continue;
            }
            worstIndividuals.add(individual);
            if(worstIndividuals.size() < numberOfIndividualsToRemove){
                Collections.sort(worstIndividuals);
            }
            else{
                Collections.sort(worstIndividuals);
                worstIndividuals.remove(worstIndividuals.size()-1);
            }
        }
        this.infeasiblePopulation.removeAll(worstIndividuals);
    }



    public void setOrderDistributionPopulation(OrderDistributionPopulation odp){
        this.orderDistributionPopulation = odp;
    }



    public static void main( String[] args){
        Data data = DataReader.loadData();
        Population population = new Population(data);
        OrderDistributionPopulation odp = new OrderDistributionPopulation(data);
        odp.initializeOrderDistributionPopulation(population);
        population.initializePopulation(odp.getRandomOrderDistribution());
        Individual individual = TournamentSelection.performSelection(population);
        individual.printDetailedFitness();
    }
}