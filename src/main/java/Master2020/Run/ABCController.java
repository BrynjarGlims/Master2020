package Master2020.Run;

import Master2020.ABC.ABCSolution;
import Master2020.ABC.PeriodicABC;
import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.DataFiles.Parameters;
import Master2020.Individual.Journey;
import Master2020.Population.PeriodicOrderDistributionPopulation;
import Master2020.StoringResults.SolutionStorer;
import gurobi.GRBException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.IntStream;

public class ABCController {




    public Data data;
    public ArrayList<PeriodicABC> swarms;
    public PeriodicOrderDistributionPopulation pod;
    public ArrayList<ABCSolution> solutions;
    public ArrayList<ABCSolution> finalSolutions;
    public CyclicBarrier downstreamGate;
    public CyclicBarrier upstreamGate;

    public PeriodicABC swarm;

    public String fileName;
    public String modelName = "ABC";
    public double time;


    public ABCController(Data data){
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
        downstreamGate = new CyclicBarrier(Parameters.numberOfSwarms + 1);
        upstreamGate = new CyclicBarrier(Parameters.numberOfSwarms + 1);
        solutions = new ArrayList<>();
        finalSolutions = new ArrayList<>();
        swarms = new ArrayList<>();
        pod = new PeriodicOrderDistributionPopulation(data);
        pod.initialize(Parameters.numberOfSwarms);
        fileName = SolutionStorer.getFolderName(this.modelName);
        for (int i = 0 ; i < Parameters.numberOfSwarms ; i++){
            PeriodicABC s = new PeriodicABC(data);
            s.initialize(pod.distributions.get(i), downstreamGate, upstreamGate);
            if(Parameters.threaded){s.start();};
            swarms.add(s);
        }
    }

    private void initializeSingular() throws GRBException {
        swarm = new PeriodicABC(data);
        swarm.initialize(swarm.orderDistribution);
    }

    private void multipleRun() throws InterruptedException, BrokenBarrierException, CloneNotSupportedException, IOException {
        if (Parameters.threaded){
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
            System.out.println("running generation: " + i);
            for (int s = 0 ; s < Parameters.numberOfSwarms ; s++){
                swarms.get(s).runIteration();
                pod.distributions.set(s, swarms.get(s).orderDistribution);
            }
            updateOrderDistributionPopulation();


        }
        for (PeriodicABC swarm : swarms){
            finalSolutions.add(swarm.storeSolution());
            swarm.terminate();
        }
        Collections.sort(finalSolutions);
        System.out.println(finalSolutions.get(0).getFitness());
    }

    private void multipleRunThreaded() throws BrokenBarrierException, InterruptedException, CloneNotSupportedException, IOException {

        for (int i = 0 ; i < Parameters.orderDistributionUpdates - 1; i++){
            System.out.println("running generation: " + i);
            runIteration();
            updateOrderDistributionPopulation();
        }
        runIteration();

        for (PeriodicABC swarm : swarms){
            finalSolutions.add(swarm.storeSolution());
            swarm.terminate();
            swarm.run = false;
        }
        downstreamGate.await();
        upstreamGate.await();
        ArrayList<Journey>[][] journeys = getJourneys();
        Collections.sort(finalSolutions);
        System.out.println(finalSolutions.get(0).getFitness());
        finalSolutions.get(0).writeSolution(fileName);
    }



    private void runIteration() throws BrokenBarrierException, InterruptedException, CloneNotSupportedException {
        //release all period swarms for
        downstreamGate.await();
        downstreamGate.reset();

        //wait for all periods to finish their generations
        upstreamGate.await();
        upstreamGate.reset();

        //update and find best order distribution
        ABCSolution  solution;
        for (int s = 0 ; s < Parameters.numberOfSwarms ; s++){
            swarms.get(s).runIteration();
            pod.distributions.set(s, swarms.get(s).orderDistribution);
            solution = swarms.get(s).storeSolution();
            System.out.println("swarm " + s + " fitness: "+ solution.getFitness() + " feasible: " + solution.feasible + " infeasibility cost: " + solution.infeasibilityCost);
        }


    }



    private void singularRun() throws BrokenBarrierException, InterruptedException, IOException, CloneNotSupportedException {
        System.out.println("RUNNING SINGULAR ABC");
        for (int i = 0 ; i < Parameters.orderDistributionUpdates ; i++){
            System.out.println("running generation: " + i);
            swarm.runIteration();
        }
        ABCSolution solution = swarm.storeSolution();
        swarm.terminate();
        System.out.println(solution.getFitness());

    }


    public void run() throws BrokenBarrierException, InterruptedException, IOException, CloneNotSupportedException {
        if (Parameters.runSingular){
            singularRun();
        }
        else {
            multipleRun();
        }
    }


    public void updateOrderDistributionPopulation() throws CloneNotSupportedException {
        solutions.clear();
        for (PeriodicABC swarm : swarms){
            swarm.minimumIterations++;
            solutions.add(swarm.storeSolution());
        }
        int[] sortedIndices = IntStream.range(0, solutions.size())
                .boxed()
                .sorted(Comparator.comparing(i -> solutions.get(i)))
                .mapToInt(i -> i)
                .toArray();
        for (int i = sortedIndices.length - 1 ; i > sortedIndices.length - Parameters.orderDistributionCutoff ; i--){
            if (swarms.get(sortedIndices[i]).minimumIterations > Parameters.minimumIterations){
                System.out.println("changing od: " + sortedIndices[i]);

//                //random OD:
//                OrderDistribution newOD = new OrderDistribution(data);
//                newOD.makeInitialDistribution();
//                pod.distributions.set(sortedIndices[i], newOD);

                //diversified new OD:
                pod.distributions.set(sortedIndices[i], pod.diversify(3));
                swarms.get(sortedIndices[i]).updateOrderDistribution(pod.distributions.get(sortedIndices[i]));
                swarms.get(sortedIndices[i]).minimumIterations = 0;
            }
        }
        for (int i = 0 ; i < swarms.size() ; i++){
            if (swarms.get(i).iterationsWithoutImprovement > Parameters.swarmIterationsWithoutImprovementLimit){
                System.out.println("MAX ITERATIONS HIT");
                finalSolutions.add(swarms.get(i).storeSolution());
                pod.distributions.set(i, pod.diversify(10));
                swarms.get(i).updateOrderDistribution(pod.distributions.get(i));
            }
        }
    }

    public ArrayList<Journey>[][] getJourneys(){
        ArrayList<Journey>[][] journeys = new ArrayList[data.numberOfPeriods][data.numberOfVehicleTypes];
        for (int p = 0 ; p < data.numberOfPeriods ; p++){
            for (int vt = 0 ; vt < data.numberOfVehicleTypes ; vt++){
                journeys[p][vt] = new ArrayList<>();
            }
        }
        ArrayList<Journey>[][] swarmJourneys;
        for (PeriodicABC swarm : swarms){
            swarmJourneys = swarm.getJourneys();
            for (int p = 0 ; p < data.numberOfPeriods ; p++){
                for (int vt = 0 ; vt < data.numberOfVehicleTypes ; vt++){
                    journeys[p][vt].addAll(swarmJourneys[p][vt]);
                }
            }
        }
        return journeys;
    }



    public static void main(String[] args) throws BrokenBarrierException, InterruptedException, GRBException, IOException, CloneNotSupportedException {
        Data data = DataReader.loadData();
        ABCController abc = new ABCController(data);
        abc.initialize();
        abc.run();
    }
}


