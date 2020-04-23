package Master2020.Population;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.Individual.Individual;
import Master2020.Individual.PeriodicIndividual;
import Master2020.ProductAllocation.OrderDistribution;

import java.util.HashSet;
import java.util.Set;

public class PeriodicPopulation {

    public Data data;
    public Population[] populations;
    public Set<PeriodicIndividual> periodicFeasibleIndividualPopulation;
    public Set<PeriodicIndividual> periodicInfeasibleIndividualPopulation;
    public OrderDistributionPopulation orderDistributionPopulation;
    int iterationsWithoutImprovement = 0;
    public OrderDistribution orderDistribution;


    public PeriodicPopulation(Data data) {
        this.data = data;
        this.populations = new Population[data.numberOfPeriods];
        for (int p = 0; p < data.numberOfPeriods; p++){
            this.populations[p] = new Population(data, p);
        }
        this.periodicFeasibleIndividualPopulation = new HashSet<PeriodicIndividual>();
        this.periodicInfeasibleIndividualPopulation = new HashSet<PeriodicIndividual>();

    }

    public void addPeriodicIndividual(PeriodicIndividual periodicIndividual){
        if (periodicIndividual.isFeasible())
            this.periodicFeasibleIndividualPopulation.add(periodicIndividual);
        else{
            this.periodicInfeasibleIndividualPopulation.add(periodicIndividual);

        }


    }

    public void initializePopulation(OrderDistribution orderDistribution){
        this.orderDistribution = orderDistribution;
        for (int p = 0; p < data.numberOfPeriods; p++) {
            this.populations[p].initializePopulation(orderDistribution);
        }
    }

    public void setIterationsWithoutImprovement(int iterations){
        this.iterationsWithoutImprovement = iterations;
    }
    public int getIterationsWithoutImprovement(){
        return this.iterationsWithoutImprovement;
    }


    public int getPopulationSize(){
        int populationSize = 0;
        for (int p = 0; p < data.numberOfPeriods; p++ ){
            populationSize += populations[p].getPopulationSize();
        }
        return populationSize;
    }


    public void improvedSurvivorSelection() {
        for (int p = 0; p < data.numberOfPeriods; p++) {
            this.populations[p].improvedSurvivorSelection();
        }
    }


    public void setOrderDistributionPopulation(OrderDistributionPopulation odp){
        this.orderDistributionPopulation = odp;
    }


    public void initializePopulations (OrderDistribution od) {
        for (int p = 0; p < data.numberOfPeriods; p++){
            this.populations[p].initializePopulation(od);
        }
    }


    public Individual returnBestIndividual(){

        // todo: implement
        return null;
    }


    public Individual returnBestFeasibleIndividual(){
        // todo: implement
        return null;
    }

    public Individual returnBestInfeasibleIndividual(){
        // todo: implement
        return null;
    }



    public static void main( String[] args){
        Data data = DataReader.loadData();
        PeriodicPopulation periodicPopulation = new PeriodicPopulation(data);
        OrderDistributionPopulation odp = new OrderDistributionPopulation(data);
        odp.initializeOrderDistributionPopulation(periodicPopulation);
        periodicPopulation.initializePopulation(odp.getRandomOrderDistribution());
        System.out.println("hei");
    }
}
