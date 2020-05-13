package Master2020.PGA;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.DataFiles.Parameters;
import Master2020.Genetic.*;
import Master2020.Individual.Individual;
import Master2020.Individual.Journey;
import Master2020.MIP.DataConverter;
import Master2020.MIP.JourneyCombinationModel;
import Master2020.MIP.OrderAllocationModel;
import Master2020.Population.PeriodicOrderDistributionPopulation;
import Master2020.Population.Population;
import Master2020.ProductAllocation.OrderDistribution;
import Master2020.Testing.ABCtests;
import Master2020.Testing.IndividualTest;
import gurobi.GRBException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class GeneticPeriodicAlgorithm extends Thread{
    public Data data;
    public Population population;
    public PeriodicPopulation periodicPopulation;
    public HashSet<Individual> repaired;
    public OrderAllocationModel orderAllocationModel;
    public JourneyCombinationModel journeyCombinationModel;
    public PeriodicIndividual bestPeriodicIndividual;
    public Individual bestIndividual;
    public List<GeneticAlgorithm> threads;
    public ArrayList<Journey>[][] journeys;
    public OrderDistribution orderDistribution;

    public double scalingFactorOrderDistribution = Parameters.initialOrderDistributionScale;
    public double fitness;
    public int numberOfIterations = 0;
    public int iterationsWithoutImprovement = 0;
    public int iterationsWithSameOd = 0;

    //threading
    public boolean run;
    private CyclicBarrier downstreamGate;
    private CyclicBarrier upstreamGate;
    private CyclicBarrier masterDownstreamGate;
    private CyclicBarrier masterUpstreamGate;

    public GeneticPeriodicAlgorithm(Data data) throws GRBException {
        this.data = data;
        orderAllocationModel = new OrderAllocationModel(data);
        journeyCombinationModel = new JourneyCombinationModel(DataConverter.convert(data));
        orderDistribution = new OrderDistribution(data);
        orderDistribution.makeInitialDistribution();
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

        System.out.println("Initialization periodic completed");
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
        periodicPopulation.addPeriodicIndividual(generateGreedyPeriodicIndividual());
        if (Parameters.useJCM){
            setJourneyFromBestIndividuals();
            createOrderDistributionFromJCM(threads, false);
        }
        else{
            makeOptimalOrderDistribution(threads ,false);
        }
        updateOrderDistributionForPopulations(orderDistribution, false);
        updateFitness();
        System.out.println("Current fitness: " + fitness);
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

    private void createOrderDistributionFromJCM(List<GeneticAlgorithm> geneticAlgorithm, boolean newOD) throws Exception {
        ArrayList<Journey>[][] arrayOfJourneys = createJourneysToJCM();

        boolean verbose = false;
        if (verbose) {
            System.out.println("all customers exists? " + ABCtests.allCustomersExists(arrayOfJourneys, data));
            System.out.println("OD valid? " + IndividualTest.testValidOrderDistribution(data, orderDistribution));
            double[] fitnesses = FitnessCalculation.getIndividualFitness(data, arrayOfJourneys, orderDistribution, 1);
            System.out.println("overload: " + fitnesses[2]);
        }

        if (journeyCombinationModel.runModel(arrayOfJourneys, orderDistribution) == 2){
            System.out.println("FOUND OPTIMAL OD!!! Ja vi elsker dette landet, som det stiger frem.");
            this.orderDistribution = journeyCombinationModel.getOrderDistribution();
            this.journeys = journeyCombinationModel.getOptimalJourneys();

        }
        else{
            System.out.println("Did not find any Optimal OD");
        }
        iterationsWithSameOd += 1;

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

    /*
    private void multipleRunThreaded() throws BrokenBarrierException, InterruptedException, CloneNotSupportedException, IOException {

        for (int i = 0 ; i < Parameters.orderDistributionUpdates ; i++){
            System.out.println("running generation: " + i);
            runIteration();
        }
        for (Swarm swarm : swarms){
            finalSolutions.add(swarm.storeSolution());
            swarm.terminate();
            swarm.run = false;
        }
        downstreamGate.await();
        upstreamGate.await();

        Collections.sort(finalSolutions);
        System.out.println(finalSolutions.get(0).getFitness());
    }

     */


    private void updateFitness(){
        double[] fitnesses = FitnessCalculation.getIndividualFitness(data, journeys, orderDistribution, 1);
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
//        ABCSolution solution = storeSolution();
//        Individual individual = HelperFunctions.createIndividual(data, journeys, orderDistribution);
//        Result result = new Result(individual, "ABC");
//        result.store();
    }

    private void makeOptimalOrderDistribution(List<GeneticAlgorithm> geneticAlgorithm, boolean newOD){
        boolean allFeasibleJourneys = setJourneyFromBestIndividuals();
        boolean verbose = false;

        if (verbose) {
            System.out.println("all customers exists? " + ABCtests.allCustomersExists(journeys, data));
            System.out.println("OD valid? " + IndividualTest.testValidOrderDistribution(data, orderDistribution));
            double[] fitnesses = FitnessCalculation.getIndividualFitness(data, journeys, orderDistribution, 1);
            System.out.println("overload: " + fitnesses[2]);
        }
        if (orderAllocationModel.createOptimalOrderDistribution(journeys, 1) == 2 && allFeasibleJourneys){
            //System.out.println("FOUND OPTIMAL OD!!! Ja vi elsker dette landet, som det stiger frem.");
            this.orderDistribution = orderAllocationModel.getOrderDistribution();
        }
        else{
            System.out.println("Did not find any Optimal OD");
        }
        iterationsWithSameOd += 1;

    }


    private PeriodicIndividual generateGreedyPeriodicIndividual(){
        PeriodicIndividual newPeriodicIndividual = new PeriodicIndividual(data);
        newPeriodicIndividual.setOrderDistribution(orderDistribution);
        for (int p = 0; p < data.numberOfPeriods; p++){
            Individual individual = periodicPopulation.populations[p].returnBestIndividual();
            if (!individual.orderDistribution.equals(orderDistribution)){
                System.out.println("------ Trying to combine individuals with diffferent ODS ------- " + individual.hashCode());
            }
            individual.updateFitness();
            if (!individual.isFeasible()){
                System.out.println("------- The added individual is not feasible -------- " + individual.hashCode());
            }
            newPeriodicIndividual.setPeriodicIndividual(individual, p);
        }
        System.out.println("Fitness: " + newPeriodicIndividual.getFitness() + " is feasible " + newPeriodicIndividual.isFeasible() + " order distribution " + orderDistribution.hashCode());
        return newPeriodicIndividual;
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

    //todo: not in use
    public void runIterations() throws Exception {
        for (int i = 0; i < Parameters.minimumUpdatesPerOrderDistributions; i++){
            System.out.println("Start iteration: " + i);
            runIteration();
        }
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

    public PGASolution storeSolution(){
        return storeSolution(false);
    }

    public PGASolution storeSolution(boolean verbose ){
        bestPeriodicIndividual = periodicPopulation.returnBestIndividual();
        PGASolution pgaSolution = bestPeriodicIndividual.createPGASolution();
        pgaSolution.individual.updateFitness();
        if (verbose)
            pgaSolution.individual.printDetailedFitness();
        return pgaSolution;
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
        periodicAlgorithm.runIteration();
    }

}
