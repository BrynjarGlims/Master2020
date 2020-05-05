package Master2020.Population;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.Individual.Individual;
import Master2020.Individual.PeriodicIndividual;
import Master2020.ProductAllocation.OrderDistribution;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class PeriodicPopulation extends Thread {

    public Data data;
    public Population[] populations;
    public Set<PeriodicIndividual> periodicFeasibleIndividualPopulation;
    public Set<PeriodicIndividual> periodicInfeasibleIndividualPopulation;
    public OrderDistributionPopulation orderDistributionPopulation;
    int iterationsWithoutImprovement = 0;
    public OrderDistribution orderDistribution;



    //threading
    public boolean run;
    private CyclicBarrier downstreamGate;
    private CyclicBarrier upstreamGate;
    private CyclicBarrier masterDownstreamGate;
    private CyclicBarrier masterUpstreamGate;


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

    public void initialize(OrderDistribution orderDistribution,  CyclicBarrier masterDownstreamGate, CyclicBarrier masterUpstreamGate ){
        this.orderDistribution = orderDistribution;
        this.masterDownstreamGate = masterDownstreamGate;
        this.masterUpstreamGate = masterUpstreamGate;
        //downstreamGate = new CyclicBarrier(data.numberOfPeriods + 1);
        //upstreamGate = new CyclicBarrier(data.numberOfPeriods + 1);


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

    public void run(){
        while (run){
            try {
                //wait for all threads to be ready
                masterDownstreamGate.await();
                //run generations
                if (run){runIteration();}
                //wait for all periods to finish
                masterUpstreamGate.await();

            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
        }
    }

    public void runIteration() throws InterruptedException, BrokenBarrierException {
        //release all period swarms for
        downstreamGate.await();
        downstreamGate.reset();

        //wait for all periods to finish their generations
        upstreamGate.await();
        upstreamGate.reset();

        //update and find best order distribution
        //makeOptimalOrderDistribution(threads);
        //updateOrderDistributionForColonies(threads, false);
        //updateFitness();

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
