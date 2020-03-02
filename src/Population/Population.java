package Population;
import DataFiles.*;
import Individual.Individual;
import Individual.AdSplit;
import ProductAllocation.OrderDistribution;

import java.util.ArrayList;

public class Population {
    private int totalPopulationSize;
    private Data data;
    private ArrayList<Individual> feasiblePopulation;
    private ArrayList<Individual> infeasiblePopulation;
    private OrderDistribution currentOrderDistribution;

    int iterationsWithoutImprovement = 0;

    public Population(Data data) {
        this.data = data;
        this.totalPopulationSize = Parameters.populationSize;
        this.currentOrderDistribution = new OrderDistribution(this.data);
        this.currentOrderDistribution.makeInitialDistribution();
        this.feasiblePopulation = new  ArrayList<Individual>();
        this.infeasiblePopulation = new  ArrayList<Individual>();
    }


    public void initializePopulation() {
        for (int i = 0; i < totalPopulationSize; i++) {
            //System.out.println("## New Individual Generated, nr: " + (i+1) + " ##" );
            Individual individual = new Individual(this.data, this);
            individual.initializeIndividual();
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


    public ArrayList<Individual> getFeasiblePopulation() {
        return feasiblePopulation;
    }

    public ArrayList<Individual> getInfeasiblePopulation() {
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
        population.initializePopulation();
        System.out.println("hei");
    }
}