package Master2020.PGA;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.Individual.Individual;
import Master2020.Population.OrderDistributionPopulation;
import Master2020.Population.Population;
import Master2020.ProductAllocation.OrderDistribution;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;

public class PeriodicPopulation extends Thread {

    public Data data;
    public Population[] populations;
    public Set<PeriodicIndividual> periodicFeasibleIndividualPopulation;
    public Set<PeriodicIndividual> periodicInfeasibleIndividualPopulation;
    public OrderDistributionPopulation orderDistributionPopulation;
    int iterationsWithoutImprovement = 0;
    public OrderDistribution orderDistribution;



    public PeriodicPopulation(Data data) {
        this.data = data;
    }

    public void initialize(OrderDistributionPopulation odp ){
        this.orderDistributionPopulation = odp;
        this.populations = new Population[data.numberOfPeriods];
        for (int p = 0; p < data.numberOfPeriods; p++){
            this.populations[p] = new Population(data, p);
            this.populations[p].setOrderDistributionPopulation(this.orderDistributionPopulation);
        }
        this.periodicFeasibleIndividualPopulation = new HashSet<PeriodicIndividual>();
        this.periodicInfeasibleIndividualPopulation = new HashSet<PeriodicIndividual>();
    }

    public void initialize(OrderDistribution orderDistribution){
        this.orderDistribution = orderDistribution;

        this.populations = new Population[data.numberOfPeriods];
        for (int p = 0; p < data.numberOfPeriods; p++){
            this.populations[p] = new Population(data, p);
            this.populations[p].setOrderDistributionPopulation(this.orderDistributionPopulation);
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

    public void setOrderDistribution(OrderDistribution orderDistribution) {
        this.orderDistribution = orderDistribution;
        for (Population population : populations){
            population.updateOrderDistributionsOfAllIndividuals(orderDistribution);
        }
    }

    public void reassignPeriodicIndividuals(){
        Set<PeriodicIndividual> tempFeasiblePopulation = new HashSet<>();
        Set<PeriodicIndividual> tempInfeasiblePopulation = new HashSet<>();
        for (PeriodicIndividual periodicIndividual : periodicFeasibleIndividualPopulation){
            if (periodicIndividual.isFeasible()){
                tempFeasiblePopulation.add(periodicIndividual);
            }
            else {
                tempInfeasiblePopulation.add(periodicIndividual);
            }
        }
        for (PeriodicIndividual periodicIndividual : periodicInfeasibleIndividualPopulation){
            if (periodicIndividual.isFeasible()){
                tempFeasiblePopulation.add(periodicIndividual);
            }
            else {
                tempInfeasiblePopulation.add(periodicIndividual);
            }
        }
        periodicFeasibleIndividualPopulation = tempFeasiblePopulation;
        periodicInfeasibleIndividualPopulation = tempInfeasiblePopulation;
        reassignIndividualsInPopulations();

    }

    private void reassignIndividualsInPopulations(){
        for (int p = 0; p < data.numberOfPeriods; p++){
            populations[p].reassignIndividualsInPopulations();
        }
    }

    public void allocateIndividual(PeriodicIndividual periodicIndividual){
        if (periodicIndividual.isFeasible()){
            this.periodicInfeasibleIndividualPopulation.remove(periodicIndividual);
            this.periodicFeasibleIndividualPopulation.add(periodicIndividual);
        }
        else{
            this.periodicInfeasibleIndividualPopulation.add(periodicIndividual);
            this.periodicFeasibleIndividualPopulation.remove(periodicIndividual);
        }
    }

    public void initializePopulations (OrderDistribution od) {
        for (int p = 0; p < data.numberOfPeriods; p++){
            this.populations[p].initializePopulation(od);
        }
    }


    public PeriodicIndividual returnBestIndividual(){
        PeriodicIndividual bestIndividual = null;
        double fitnessScore = Double.MAX_VALUE;
        for (PeriodicIndividual periodicIndividual : periodicFeasibleIndividualPopulation){
            if (periodicIndividual.getFitness() < fitnessScore){
                bestIndividual = periodicIndividual;
                fitnessScore = periodicIndividual.getFitness();
            }
        }
        if (bestIndividual != null){
            return bestIndividual;
        }
        for (PeriodicIndividual periodicIndividual : periodicInfeasibleIndividualPopulation){
            if (periodicIndividual.getFitness() < fitnessScore){
                bestIndividual = periodicIndividual;
                fitnessScore = periodicIndividual.getFitness();
            }
        }
        return bestIndividual;
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
        OrderDistributionPopulation odp = new OrderDistributionPopulation(data);
        PeriodicPopulation periodicPopulation = new PeriodicPopulation(data);
        periodicPopulation.initialize(odp);

        odp.initializeOrderDistributionPopulation(periodicPopulation);
        periodicPopulation.initializePopulation(odp.getRandomOrderDistribution());
        System.out.println("hei");
    }
}
