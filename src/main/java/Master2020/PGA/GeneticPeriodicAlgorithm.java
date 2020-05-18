package Master2020.PGA;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.DataFiles.Parameters;
import Master2020.Genetic.*;
import Master2020.Individual.Individual;
import Master2020.Individual.Journey;
import Master2020.Interfaces.PeriodicAlgorithm;
import Master2020.MIP.DataConverter;
import Master2020.MIP.JourneyCombinationModel;
import Master2020.MIP.OrderAllocationModel;
import Master2020.Population.PeriodicOrderDistributionPopulation;
import Master2020.Population.Population;
import Master2020.ProductAllocation.OrderDistribution;
import Master2020.Testing.ABCtests;
import Master2020.Testing.IndividualTest;
import gurobi.GRBException;
import org.nustaq.kson.KsonStringOutput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class GeneticPeriodicAlgorithm extends Thread implements PeriodicAlgorithm {
    public Data data;
    public Population population;
    public PeriodicPopulation periodicPopulation;
    public HashSet<Individual> repaired;
    public OrderAllocationModel orderAllocationModel;
    public List<GeneticAlgorithm> threads;
    public ArrayList<Journey>[][] journeys;
    public OrderDistribution orderDistribution;

    public double scalingFactorOrderDistribution = Parameters.initialOrderDistributionScale;
    public double fitness;
    public int numberOfIterations = 0;
    public int iterationsWithoutImprovement = 0;
    public int iterationsWithSameOd = 0;
    public int minimumIterations;
    public double startTime;
    public double firstIterationTime;

    //threading
    public boolean run;
    private CyclicBarrier downstreamGate;
    private CyclicBarrier upstreamGate;
    private CyclicBarrier masterDownstreamGate;
    private CyclicBarrier masterUpstreamGate;

    public double timeWarpPenalty;
    public double overLoadPenalty;

    public GeneticPeriodicAlgorithm(Data data) throws GRBException {
        this.data = data;
        orderAllocationModel = new OrderAllocationModel(data);
        orderDistribution = new OrderDistribution(data);
        orderDistribution.makeInitialDistribution();
        this.timeWarpPenalty = Parameters.initialTimeWarpPenalty;
        this.overLoadPenalty = Parameters.initialOverLoadPenalty;
    }

    public void initialize(OrderDistribution orderDistribution) throws GRBException {
        System.out.println("Initialize periodic population..");
        Parameters.isPeriodic = true;
        this.orderDistribution = orderDistribution;
        periodicPopulation = new PeriodicPopulation(data);
        periodicPopulation.initialize(orderDistribution);
        BiasedFitness.setBiasedFitnessScore(periodicPopulation);
        repaired = new HashSet<>();
        fitness = Double.MAX_VALUE;
        numberOfIterations = 0;
        iterationsWithoutImprovement = 0;
        iterationsWithSameOd = 0;
        minimumIterations = 0;
        //threading
        downstreamGate = new CyclicBarrier(data.numberOfPeriods + 1);
        upstreamGate = new CyclicBarrier(data.numberOfPeriods + 1);
        threads = makeThreadsAndStart(downstreamGate, upstreamGate);
        this.run = true;
    }

    public void initialize(OrderDistribution orderDistribution, CyclicBarrier masterDownstreamGate, CyclicBarrier masterUpstreamGate) throws GRBException {
        this.initialize(orderDistribution);
        //threading
        this.masterDownstreamGate = masterDownstreamGate;
        this.masterUpstreamGate = masterUpstreamGate;
    }

    public OrderDistribution getOrderDistribution() {
        return this.orderDistribution;
    }

    private List<GeneticAlgorithm> makeThreadsAndStart(CyclicBarrier downstreamGate, CyclicBarrier upstreamGate) throws GRBException { // TODO: 06/05/2020 This exception is not in swatm
        List<GeneticAlgorithm> threads = new ArrayList<>();
        for (int p = 0 ; p < data.numberOfPeriods ; p++){
            GeneticAlgorithm algorithm = new GeneticAlgorithm(data);
            algorithm.initialize(p, this.orderDistribution, downstreamGate, upstreamGate, periodicPopulation.populations[p]);
            threads.add(algorithm);
        }
        for (Thread t : threads){
            t.start();
        }
        return threads;
    }

    public ArrayList<Journey>[][] createJourneysToJCM(){
        ArrayList<Journey>[][] journeys = new ArrayList[data.numberOfPeriods][data.numberOfVehicleTypes];
        for (int p = 0; p < data.numberOfPeriods; p++){
            journeys[p] = threads.get(p).getBestJourneysFromIndividuals();
        }
        return journeys;
    }

    public void runIteration() throws Exception {


        downstreamGate.await();
        downstreamGate.reset();

        //wait for all periods to finish their generations
        upstreamGate.await();
        upstreamGate.reset();

        //System.out.println("Updating order distribution");
        //periodicPopulation.addPeriodicIndividual(generateGreedyPeriodicIndividual());


        if (Parameters.useODMIPBetweenGenerations){
            if ((System.currentTimeMillis() - startTime ) < Parameters.timeLimitPerAlgorithm){  //todo: evaluate this
            makeOptimalOrderDistribution(threads ,false);
            updateOrderDistributionForPopulations(orderDistribution, false);
            }
            else{
                setJourneyFromBestIndividuals();
            }
        }
        //System.out.println("Time: :" +  Double.toString(System.currentTimeMillis() - startTime));
        updateFitness();
    }

    private boolean setJourneyFromBestIndividuals() {
        journeys = new ArrayList[data.numberOfPeriods][data.numberOfVehicleTypes];
        boolean allFeasibleJourneys = true;
        for (int p = 0; p < data.numberOfPeriods; p++) {
            Individual individual = periodicPopulation.populations[p].returnBestIndividual();
            if (!individual.isFeasible())
                allFeasibleJourneys = false;
            for (int vt = 0; vt < data.numberOfVehicleTypes; vt++) {
                journeys[p][vt] = individual.journeyList[0][vt];

            }
        }
        return allFeasibleJourneys;
    }


    public void resetPeriodicPopulation() {
        periodicPopulation = new PeriodicPopulation(data);
        periodicPopulation.initialize(orderDistribution);
        numberOfIterations = 0;
        iterationsWithoutImprovement = 0;
        iterationsWithSameOd = 0;
        repaired = new HashSet<>();
        fitness = Double.MAX_VALUE;
        BiasedFitness.setBiasedFitnessScore(periodicPopulation);


        for (int p = 0; p < data.numberOfPeriods; p++) {
            threads.get(p).setPopulation(periodicPopulation.populations[p]);
            threads.get(p).resetCounters();
        }


    }

    public void updateOrderDistribution(OrderDistribution orderDistribution){
        this.orderDistribution = orderDistribution;
        periodicPopulation.orderDistribution = orderDistribution;
        updateOrderDistributionForPopulations(orderDistribution, true);
        iterationsWithoutImprovement = 0;
    }

    private void updateOrderDistributionForPopulations(OrderDistribution orderDistribution, boolean newOD){
        for (GeneticAlgorithm algorithm : threads){
            algorithm.orderDistribution = orderDistribution;
            algorithm.population.updateOrderDistributionsOfAllIndividuals(this.orderDistribution);
            algorithm.population.reassignIndividualsInPopulations();
            if (newOD){
                resetPeriodicPopulation();
            }
        }
    }

    private void updateFitness(){
        double[] fitnesses = FitnessCalculation.getIndividualFitness(data, journeys, orderDistribution, 1, Parameters.initialTimeWarpPenalty, Parameters.initialOverLoadPenalty);
        double fitness = 0;
        for (double d : fitnesses){
            fitness += d;
        }
        if (fitness < this.fitness){
            iterationsWithoutImprovement = 0;
        }
        else {
            iterationsWithoutImprovement++;
        }
        this.fitness = fitness;
    }

    public void terminate() throws BrokenBarrierException, InterruptedException, CloneNotSupportedException, IOException {
        //terminate threads
        for (GeneticAlgorithm algorithm : threads){
            algorithm.run = false;
        }
        downstreamGate.await();
        upstreamGate.await();

    }

    private void makeOptimalOrderDistribution(List<GeneticAlgorithm> geneticAlgorithm, boolean newOD){
        boolean allFeasibleJourneys = setJourneyFromBestIndividuals();
        boolean verbose = false;

        if (verbose) {
            System.out.println("all customers exists? " + ABCtests.allCustomersExists(journeys, data));
            System.out.println("OD valid? " + IndividualTest.testValidOrderDistribution(data, orderDistribution));
            double[] fitnesses = FitnessCalculation.getIndividualFitness(data, journeys, orderDistribution, 1, Parameters.initialTimeWarpPenalty, Parameters.initialOverLoadPenalty);
            System.out.println("overload: " + fitnesses[2]);
        }
        if (orderAllocationModel.createOptimalOrderDistribution(journeys, 1) == 2 && allFeasibleJourneys){
            // System.out.println("New optimal od found");
            this.orderDistribution = orderAllocationModel.getOrderDistribution();
        }
        iterationsWithSameOd += 1;

    }

    public void run(){
        while (run){
            try {
                //wait for all threads to be ready
                masterDownstreamGate.await();
                //run generations
                if (run){runIterations();}
                //wait for all periods to finish
                masterUpstreamGate.await();

            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public void runIterations() throws Exception {
        setStartTimeOfThreads();

        for (int i = 0; i < Parameters.minimumUpdatesPerOrderDistributions; i++){
            //System.out.println("Start odgeneration " + i);
            if ((System.currentTimeMillis() - this.startTime) < Parameters.timeLimitPerAlgorithm) {
                runIteration();
            }
            if (i == 0){
                firstIterationTime = (System.currentTimeMillis() - startTime);
            }
        }
    }

    public void setStartTimeOfThreads(){
        startTime = System.currentTimeMillis();
        for (GeneticAlgorithm algorithm : threads){
            algorithm.startTime = this.startTime;
        }
    }

    public ArrayList<Journey>[][] getJourneys(){
        return journeys;
    }

    private void updateOrderDistributionScalingParameter() {
        if (Parameters.initialOrderDistributionScale == 1){
            return;
        }
        if (numberOfIterations % Parameters.numberOfGenerationsBetweenODScaling == 0 && numberOfIterations > Parameters.numberOfGenerationBeforeODScalingStarts) {
            scalingFactorOrderDistribution = (scalingFactorOrderDistribution < 1) ? Parameters.incrementPerOrderDistributionScaling + scalingFactorOrderDistribution : 1;
            orderDistribution.setOrderScalingFactor(scalingFactorOrderDistribution);
            periodicPopulation.reassignPeriodicIndividuals();
            System.out.println("############# CURRENT ORDER DISTRIBUTION SCALING IS " + scalingFactorOrderDistribution + "################");
        }
    }

    public ArrayList<Journey>[][] getOptimalJourneyFromThreads(){
        this.journeys = new ArrayList[data.numberOfPeriods][data.numberOfVehicleTypes];
        for (GeneticAlgorithm algorithm : threads){
            Individual individual =  algorithm.population.returnBestIndividual();
            if (!individual.isFeasible()) {
                System.out.println("Individual is feasible: " + individual.isFeasible());
                System.out.println("Individual added is not feasible");
            }
            this.journeys[algorithm.period] = individual.journeyList[0];
        }
        return this.journeys;

    }

    public PGASolution storeSolution() throws CloneNotSupportedException {
        return storeSolution(false);
    }

    public PGASolution storeSolution(boolean verbose) throws CloneNotSupportedException {
        journeys = getOptimalJourneyFromThreads();
        PGASolution pgaSolution = new PGASolution(orderDistribution.clone(), journeys);  //// TODO: 14/05/2020 Implement correctly
        return pgaSolution;
    }

    public void setRun(boolean run){
        this.run = run;
    }

    public void setMinimumIterations(int value){
        this.minimumIterations = value;
    }

    public int getMinimumIterations(){
        return minimumIterations;
    }

    public int getIterationsWithoutImprovement(){
        return iterationsWithoutImprovement;
    }

    public double getIterationTime(){
        return this.firstIterationTime;
    }

    public static void main(String[] args) throws Exception {
        Data data = DataReader.loadData();
        GeneticPeriodicAlgorithm periodicAlgorithm = new GeneticPeriodicAlgorithm(data);
        OrderDistribution od = new OrderDistribution(data);
        od.makeInitialDistribution();
        OrderDistribution copy = od.clone();
        System.out.println(od.diversityScore(copy));
        OrderDistribution od2 = new OrderDistribution(data);
        od2.makeInitialDistribution();
        System.out.println(od.diversityScore(od2));

        PeriodicOrderDistributionPopulation pod = new PeriodicOrderDistributionPopulation(data);

        pod.initialize(5);
        OrderDistribution div = pod.diversify(10);
        periodicAlgorithm.initialize(div);
        periodicAlgorithm.runIterations();
    }

}
