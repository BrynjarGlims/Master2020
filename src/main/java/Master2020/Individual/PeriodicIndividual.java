package Master2020.Individual;

import Master2020.DataFiles.Data;
import Master2020.Population.PeriodicPopulation;

public class PeriodicIndividual {

    public Data data;
    public Individual[] individuals;


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


}
