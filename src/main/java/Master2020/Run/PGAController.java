package Master2020.Run;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.DataFiles.Parameters;
import Master2020.PGA.GeneticPeriodicAlgorithm;
import Master2020.PGA.PGASolution;
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

    private void multipleRun() throws Exception {
        if (Parameters.threadedGA){
            multipleRunThreaded();
        }
        else{
            //multipleRunSingleThread();
        }
    }


    private void multipleRunThreaded() throws Exception {

        for (int i = 0 ; i < Parameters.orderDistributionUpdatesGA - 1; i++){
            System.out.println("Running generation: " + i);
            runIteration();
            updateOrderDistributionPopulation();
        }
        runIteration();

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



    private void runIteration() throws Exception {
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


    public void run() throws Exception {
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
                OrderDistribution od;
                if ((Math.random() < Parameters.diversifiedODProbability)) {
                    od = pod.diversify(10);
                }
                else{
                    od = new OrderDistribution(data);
                    od.makeInitialDistribution();
                }

                //initialize if new od
                periodicAlgorithmsArrayList.get(i).updateOrderDistribution(od);
                periodicAlgorithmsArrayList.get(i).resetPeriodicPopulation();


            }
        }


    }



    public static void main(String[] args) throws Exception {
        Data data = DataReader.loadData();
        PGAController pga = new PGAController(data);
        pga.initialize();
        pga.run();
    }
}




