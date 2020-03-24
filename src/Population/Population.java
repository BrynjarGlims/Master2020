package Population;
import DataFiles.*;
import Genetic.TournamentSelection;
import Individual.Individual;
import Individual.AdSplit;
import ProductAllocation.OrderDistribution;


import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.*;


public class Population {
    private int totalPopulationSize;
    public Data data;
    public Set<Individual> feasiblePopulation;
    public Set<Individual> infeasiblePopulation;
    public OrderDistributionPopulation orderDistributionPopulation;

    int iterationsWithoutImprovement = 0;

    public Population(Data data) {
        this.data = data;
        this.orderDistributionPopulation = orderDistributionPopulation;
        this.feasiblePopulation = new HashSet<Individual>();
        this.infeasiblePopulation = new HashSet<Individual>();
    }

    public int getPopulationSize(){
        return feasiblePopulation.size() + infeasiblePopulation.size();
    }

    public void survivorSelection(){
        this.reduceFeasiblePopulation();
        this.reduceInfeasiblePopulation();
    }


    private void reduceFeasiblePopulation(){
        int numberOfIndividualsToRemove = feasiblePopulation.size() - Parameters.minimumSubIndividualPopulationSize;
        if (numberOfIndividualsToRemove < 0)
            return;
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


    public void initializePopulation (OrderDistribution od) {
        for (int i = 0; i < Parameters.initialPopulationSize; i++) {
            Individual individual = new Individual(this.data, this);
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

    public void setSurvivorsForNextGeneration(){
        ArrayList<Individual> listToBeSorted = new ArrayList<Individual>(this.infeasiblePopulation);
        listToBeSorted.addAll(this.feasiblePopulation);
        //System.out.println(listToBeSorted.size());
        //System.out.println(this.feasiblePopulation.size());
        //System.out.println(this.infeasiblePopulation.size());
        Collections.sort(listToBeSorted);
        int counter = 0;
        for (Individual individual : listToBeSorted){
            if (counter < Parameters.numberOfElitismSurvivorsPerGeneration){
                individual.isSurvivor = true;
                counter++;
            }
            else{
                individual.isSurvivor = false;
            }
        }
    }


    private static double getFitnessDifference(Individual i1, Individual i2) {
        return (Math.abs(i1.fitness - i2.fitness));
    }


    private Set<Individual> getFeasibleClonesForAnIndividual(Individual individual) {
        Set<Individual> setOfClones = new HashSet<Individual>();
        for (Individual ind: feasiblePopulation)
            if (getFitnessDifference(individual, ind) <= Parameters.minimumFitnessDifferenceForClones) {
                setOfClones.add(ind);
            }
        return setOfClones;
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


    public Set<Individual> getFeasiblePopulation() {
        return feasiblePopulation;
    }

    public Set<Individual> getInfeasiblePopulation() {
        return infeasiblePopulation;
    }

    public HashSet<Individual> getTotalPopulation(){
        HashSet<Individual> populationSet = new HashSet<Individual>(feasiblePopulation);
        populationSet.addAll(infeasiblePopulation);
        return populationSet;
    }

    public int getSizeOfInfeasiblePopulation() {
        int sizeOfInfeasiblePopulation = 50;
        return sizeOfInfeasiblePopulation;
    }

    public int getSizeOfFeasiblePopulation() {
        int sizeOfFeasiblePopulation = 50;
        return sizeOfFeasiblePopulation;
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

    public int getIterationsWithoutImprovement(){
        return iterationsWithoutImprovement;
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