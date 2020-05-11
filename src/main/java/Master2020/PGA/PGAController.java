package Master2020.PGA;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.DataFiles.Parameters;
import Master2020.Population.PeriodicOrderDistributionPopulation;
import Master2020.ProductAllocation.OrderDistribution;
import gurobi.GRBException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.IntStream;

public class PGAController {

    public Data data;
    public ArrayList<GeneticPeriodicAlgorithm> periodicAlgorithmsArrayList;
    public PeriodicOrderDistributionPopulation pod;
    public ArrayList<PGASolution> solutions;
    public ArrayList<PGASolution> finalSolutions;
    public CyclicBarrier downstreamGate;
    public CyclicBarrier upstreamGate;
    public GeneticPeriodicAlgorithm periodicAlgorithm;



    public PGAController(Data data){
        this.data = data;
    }

    public void initialize() throws GRBException {
        if (Parameters.runSingular){
            initializeSingular();
        }
        else {
            initializeMultiple();
        }
    }

    private void initializeMultiple() throws GRBException {
        downstreamGate = new CyclicBarrier(Parameters.numberOfPeriodicParallels + 1);
        upstreamGate = new CyclicBarrier(Parameters.numberOfPeriodicParallels + 1);
        solutions = new ArrayList<>();
        finalSolutions = new ArrayList<>();
        periodicAlgorithmsArrayList = new ArrayList<>();
        pod = new PeriodicOrderDistributionPopulation(data);
        pod.initialize(Parameters.numberOfPeriodicParallels);
        for (int i = 0; i < Parameters.numberOfPeriodicParallels; i++){
            GeneticPeriodicAlgorithm p = new GeneticPeriodicAlgorithm(data);
            p.initialize(pod.distributions.get(i), downstreamGate, upstreamGate);
            if(Parameters.threadedPGA){p.start();}
            periodicAlgorithmsArrayList.add(p);
        }
    }

    private void initializeSingular() throws GRBException {
        periodicAlgorithm = new GeneticPeriodicAlgorithm(data);
        periodicAlgorithm.initialize(periodicAlgorithm.orderDistribution);

    }

    private void multipleRun() throws InterruptedException, BrokenBarrierException, CloneNotSupportedException, IOException, GRBException {
        if (Parameters.threadedGA){
            multipleRunThreaded();
        }
        else{
            multipleRunSingleThread();
        }
    }


    private void multipleRunSingleThread() throws InterruptedException, BrokenBarrierException, CloneNotSupportedException, IOException {
        System.out.println("RUNNING MULTIPLE ABC");
        // TODO: 01.05.2020 diversify, run, scale down, diversify
        // TODO: 01.05.2020 converged solutions stored somewhere else?

        for (int i = 0 ; i < Parameters.orderDistributionUpdates ; i++){
            System.out.println("Running generation: " + i);
            //for (int s = 0 ; s < Parameters.numberOfSwarms ; s++){
            //    swarms.get(s).runIteration();
            //    pod.distributions.set(s, swarms.get(s).orderDistribution);
            //}
            updateOrderDistributionPopulation();


        }

        for (GeneticPeriodicAlgorithm algorithm : periodicAlgorithmsArrayList){
            finalSolutions.add(algorithm.storeSolution(true));
            algorithm.terminate();
        }


        Collections.sort(finalSolutions);
        System.out.println(finalSolutions.get(0).getFitness());
    }

    private void multipleRunThreaded() throws BrokenBarrierException, InterruptedException, CloneNotSupportedException, IOException, GRBException {

        for (int i = 0 ; i < Parameters.orderDistributionUpdatesGA ; i++){
            System.out.println("Running generation: " + i);
            runIteration();
        }

        for (GeneticPeriodicAlgorithm algorithm : periodicAlgorithmsArrayList){
            finalSolutions.add(algorithm.storeSolution(true));
            algorithm.terminate();
            algorithm.run = false;
        }


        downstreamGate.await();
        upstreamGate.await();

        Collections.sort(finalSolutions);
        //System.out.println(finalSolutions.get(0).getFitness());
    }



    private void runIteration() throws BrokenBarrierException, InterruptedException, CloneNotSupportedException, IOException, GRBException {
        //release all period swarms for
        downstreamGate.await();
        downstreamGate.reset();

        //wait for all periods to finish their generations
        upstreamGate.await();
        upstreamGate.reset();

        //update and find best order distribution
        for (int p = 0; p < Parameters.numberOfPeriodicParallels; p++){
            periodicAlgorithmsArrayList.get(p).runIteration();
            solutions.add(periodicAlgorithmsArrayList.get(p).storeSolution());  //store solution : to implement
            pod.distributions.set(p, periodicAlgorithmsArrayList.get(p).getOrderDistribution());
        }
        updateOrderDistributionPopulation();




    }



    private void singularRun() throws BrokenBarrierException, InterruptedException, IOException, CloneNotSupportedException {
        System.out.println("RUNNING SINGULAR ABC");
        /*
        for (int i = 0 ; i < Parameters.orderDistributionUpdates ; i++){
            System.out.println("running generation: " + i);
            swarm.runIteration();
        }
        ABCSolution solution = swarm.storeSolution();
        swarm.terminate();
        System.out.println(solution.getFitness());

         */

    }


    public void run() throws BrokenBarrierException, InterruptedException, IOException, CloneNotSupportedException, GRBException {
        if (Parameters.runSingularGA){
            singularRun();
        }
        else {
            multipleRun();
        }
    }





    public void updateOrderDistributionPopulation() throws CloneNotSupportedException {

        solutions.clear();
        for (GeneticPeriodicAlgorithm gpa : periodicAlgorithmsArrayList){
            solutions.add(gpa.storeSolution());  //todo: implement
        }
        int[] sortedIndices = IntStream.range(0, solutions.size())
                .boxed()
                .sorted(Comparator.comparing(i -> solutions.get(i)))
                .mapToInt(i -> i)
                .toArray();
        for (int i = sortedIndices.length - 1 ; i > sortedIndices.length - Parameters.orderDistributionCutoff ; i--){
            pod.distributions.set(sortedIndices[i], pod.diversify(10));
            periodicAlgorithmsArrayList.get(sortedIndices[i]).updateOrderDistribution(pod.distributions.get(sortedIndices[i]));
        }
        for (int i = 0 ; i < periodicAlgorithmsArrayList.size() ; i++){
            if (periodicAlgorithmsArrayList.get(i).iterationsWithSameOd > Parameters.minimumUpdatesPerOrderDistributions){
                System.out.println("MAX ITERATIONS HIT - RESET POPULATION AND ASSIGN NEW OD");
                finalSolutions.add(periodicAlgorithmsArrayList.get(i).storeSolution());
                OrderDistribution od = (Math.random() < Parameters.diversifiedODProbability) ? pod.diversify(10) : new OrderDistribution(data); //todo :initialize..,.
                //initialize if new od
                periodicAlgorithmsArrayList.get(i).updateOrderDistribution(od);
                periodicAlgorithmsArrayList.get(i).resetPeriodicPopulation();


            }
        }


    }



    public static void main(String[] args) throws BrokenBarrierException, InterruptedException, GRBException, IOException, CloneNotSupportedException {
        Data data = DataReader.loadData();
        PGAController pga = new PGAController(data);
        pga.initialize();
        pga.run();
    }
}




