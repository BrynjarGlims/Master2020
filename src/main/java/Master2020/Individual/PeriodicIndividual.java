package Master2020.Individual;

import Master2020.DataFiles.Data;
import Master2020.Population.PeriodicPopulation;
import gurobi.GRB;

public class PeriodicIndividual {

    public Data data;
    public Individual[] individuals;
    public double fitness = Double.MAX_VALUE;


    public PeriodicIndividual(Data data){
        this.data = data;
    }

    public void setPeriodicIndividual(Individual individual, int p){
        this.individuals[p] = individual;
    }

    public Individual getPeriodicIndividual(int p){
        if (this.individuals[p] == null){
            System.out.println("Individual not set");
            return null;
        }
        else{
            return this.individuals[p];
        }
    }

    public double getFitness() {
        this.fitness = 0;
        for (int p = 0; p < data.numberOfPeriods; p++){
            this.fitness += this.individuals[p].getFitness(true);
        }


        return
    }
}
