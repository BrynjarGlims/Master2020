package Population;
import DataFiles.*;
import Individual.Individual;
import Individual.AdSplit;
import ProductAllocation.OrderDistribution;
import scala.xml.PrettyPrinter;


import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.*;


public class Population {
    private int totalPopulationSize;
    private Data data;
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



    public void initializePopulation (OrderDistributionPopulation odp) {
        this.orderDistributionPopulation = odp;
        for (int i = 0; i < Parameters.initialPopulationSize; i++) {

            Individual individual = new Individual(this.data, this);
            individual.initializeIndividual();
            AdSplit.adSplitPlural(individual);
            individual.updateFitness();

            if (individual.isFeasible()) {
                feasiblePopulation.add(individual);
                if (getSizeOfFeasiblePopulation() > Parameters.maximumSubPopulationSize) {
                    //survivorSelection();
                }
            }
            else {
                infeasiblePopulation.add(individual);
                if (getSizeOfInfeasiblePopulation() > Parameters.maximumSubPopulationSize) {
                    //activate survivor selection
                }
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



    public void selectFeasibleSurvivors() {
        Set<Individual> setOfAllClones = new HashSet<Individual>();
        //select #Individuals (size = minimumSubPopulationSize) to keep both diversity and low-cost individuals
        for (Individual individual: feasiblePopulation) {
            for (Individual ind: getFeasibleClonesForAnIndividual(individual)) {
                setOfAllClones.add(ind);
            }
        }

        for (int i = 0; i < (getSizeOfFeasiblePopulation() - Parameters.minimumSubPopulationSize); i++) {
            //setOfAllClones.sort(); //TODO: find a way to sort individuals based on fitness
        }
        //X = set of individuals with clones
        //if X not empty:
        //remove individuals in X with max biased fitness
        //else:
        //remove P in the whole subpop with maximum biased fitness
        //update distance and biased fitness measures
    }

    public void selectInfeasibleSurvivors() {
        Set<Individual> setOfAllClones = new HashSet<Individual>();
        for (Individual individual: feasiblePopulation) {
            for (Individual ind: getFeasibleClonesForAnIndividual(individual)) {
                setOfAllClones.add(ind);
            }
        }

        for (int i = 0; i < (getSizeOfInfeasiblePopulation() - Parameters.minimumSubPopulationSize); i++) {
            //setOfAllClones.sort(); //TODO: find a way to sort individuals based on fitness
        }

    }


    public Set<Individual> getFeasiblePopulation() {
        return feasiblePopulation;
    }

    public Set<Individual> getInfeasiblePopulation() {
        return infeasiblePopulation;
    }

    public int getTotalPopulationSize() {
        return totalPopulationSize;
    }

    public int getSizeOfInfeasiblePopulation() {
        int sizeOfInfeasiblePopulation = 50;
        return sizeOfInfeasiblePopulation;
    }

    public int getSizeOfFeasiblePopulation() {
        int sizeOfFeasiblePopulation = 50;
        return sizeOfFeasiblePopulation;
    }

    public int getIterationsWithoutImprovement(){
        return iterationsWithoutImprovement;
    }

    public static void main( String[] args){
        Data data = DataReader.loadData();
        Population population = new Population(data);
        OrderDistributionPopulation odp = new OrderDistributionPopulation(data);
        odp.initializeOrderDistributionPopulation(population);
        population.initializePopulation(odp);
        System.out.println("hei");
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
}