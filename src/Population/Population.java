package Population;
import DataFiles.*;
import Individual.Individual;

import java.util.ArrayList;

public class Population {
    private int feasiblePopulationSize;
    private int infeasiblePopulationSize;
    private int totalPopulationSize;
    private Data data;
    private ArrayList<Individual> feasiblePopulation;
    private ArrayList<Individual> infeasiblePopulation;

    public Population(int feasiblePopulationSize, int infeasiblePopulationSize, int totalPopulationSize, Data data) {
        this.data = data;
        this.totalPopulationSize = totalPopulationSize;
        //this.initializePopulation();
        this.feasiblePopulationSize = feasiblePopulationSize;
        this.infeasiblePopulationSize = infeasiblePopulationSize;
    }


    /*
    public void initializePopulation() {
        for (int i = 0; i < totalPopulationSize; i++) {
            Individual individual = new Individual(data);
            if (individual.isFeasible()) {
                feasiblePopulation.add(individual);
            }
            else {
                infeasiblePopulation.add(individual);
            }
        }
    }

     */

    public ArrayList<Individual> getFeasiblePopulation() {
        return feasiblePopulation;
    }

    public ArrayList<Individual> getInfeasiblePopulation() {
        return infeasiblePopulation;
    }
}
