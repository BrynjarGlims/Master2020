package Population;
import DataFiles.*;
import Individual.Individual;
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
        this.currentOrderDistribution.makeDistribution();
        this.feasiblePopulation = new  ArrayList<Individual>();
        this.infeasiblePopulation = new  ArrayList<Individual>();
    }


    public void initializePopulation() {
        for (int i = 0; i < totalPopulationSize; i++) {
            Individual individual = new Individual(this.data, this.currentOrderDistribution);
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

    public int getIterationsWithoutImprovement(){
        return iterationsWithoutImprovement;
    }
}
