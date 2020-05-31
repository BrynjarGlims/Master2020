package Master2020.Run;

import Master2020.ABC.PeriodicABC;
import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.DataFiles.Parameters;
import Master2020.Individual.Journey;
import Master2020.Interfaces.PeriodicAlgorithm;
import Master2020.Interfaces.PeriodicSolution;
import Master2020.MIP.JCMSolution;
import Master2020.MIP.JourneyCombinationModel;
import Master2020.PGA.GeneticPeriodicAlgorithm;
import Master2020.Population.PeriodicOrderDistributionPopulation;
import Master2020.ProductAllocation.OrderDistribution;
import Master2020.StoringResults.SolutionStorer;
import Master2020.Testing.HybridTest;
import Master2020.Testing.IndividualTest;
import Master2020.Testing.MIPTest;
import Master2020.Testing.SolutionTest;
import Master2020.Utils.Utils;
import gurobi.GRBException;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.IntStream;

public class HybridController {

    public static int algorithmCounter;
    public Data data;
    public ArrayList<PeriodicAlgorithm> algorithms;
    public PeriodicOrderDistributionPopulation pod;
    public ArrayList<PeriodicSolution> solutions;
    public ArrayList<PeriodicSolution> finalSolutions;
    public JourneyCombinationModel journeyCombinationModel;
    public OrderDistribution orderDistributionJCM;
    public CyclicBarrier downstreamGate;
    public CyclicBarrier upstreamGate;
    public String fileName;
    public String modelName = "HYBRID";
    public static long startTime;
    public double currentBestSolution;
    public int iterationsWithoutImprovement;

    public double bestIterationFitness;
    public PeriodicSolution bestIterationSolution;

    public HybridController() throws GRBException {
        algorithmCounter = 0;
        this.data = DataReader.loadData();
        initialize();
    }

    public void initialize() throws GRBException {
        startTime = System.currentTimeMillis();
        currentBestSolution = Double.MAX_VALUE;
        iterationsWithoutImprovement = 0;
        downstreamGate = new CyclicBarrier(Parameters.numberOfAlgorithms + 1);
        upstreamGate = new CyclicBarrier(Parameters.numberOfAlgorithms + 1);
        solutions = new ArrayList<>();
        finalSolutions = new ArrayList<>();
        algorithms = new ArrayList<>();
        pod = new PeriodicOrderDistributionPopulation(data);
        pod.initialize(Parameters.numberOfAlgorithms);
        journeyCombinationModel = new JourneyCombinationModel(data);
        fileName = SolutionStorer.getFolderName(this.modelName);
        for (int i = 0 ; i < Parameters.numberOfPGA ; i++){
            GeneticPeriodicAlgorithm s = new GeneticPeriodicAlgorithm(data);
            s.initialize(pod.distributions.get(i), downstreamGate, upstreamGate);
            s.start();
            algorithms.add(s);
        }
        for (int i = Parameters.numberOfPGA ; i < Parameters.numberOfAlgorithms ; i++){
            PeriodicABC s = new PeriodicABC(data);
            s.initialize(pod.distributions.get(i), downstreamGate, upstreamGate);
            s.start();
            algorithms.add(s);
        }
    }

    public void terminate() throws GRBException {
        if (Parameters.useJCM){
            journeyCombinationModel.terminateModel();
        }
    }

    public void run() throws Exception {
        int genCounter = 0;
        while (System.currentTimeMillis() - startTime < Parameters.totalRuntime && iterationsWithoutImprovement < Parameters.maxNumberIterationsWithoutImprovement){

            if ((startTime + Parameters.totalRuntime) - System.currentTimeMillis() < Parameters.timeLimitPerAlgorithm){
                Parameters.useODMIPBetweenIterations = false;
                Parameters.timeLimitPerAlgorithm = System.currentTimeMillis() - startTime;
            }
            System.out.println(" ### Running generation: " + genCounter + " ### ");
            runIteration();
            if (Parameters.useJCM) {
                generateOptimalSolution();
            }
            storeBestCurrentSolution();
            updateItertionsWithoutImprovement();
            updateOrderDistributionPopulation();
            if (Parameters.dynamicRuntime){
                updateRuntimeOfThreads();
            }
            genCounter++;
        }
        runIteration();
        if (Parameters.useJCM)
            generateOptimalSolution();

        for (PeriodicAlgorithm algorithm : algorithms){
            finalSolutions.add(algorithm.storeSolution());
            algorithm.terminate();
            algorithm.setRun(false);
        }
        downstreamGate.await();
        upstreamGate.await();

        Collections.sort(finalSolutions);
        System.out.println("The fitness of the final solution is: " + finalSolutions.get(0).getFitness());
        finalSolutions.get(0).writeSolution(fileName, this.startTime);
    }

    public void updateRuntimeOfThreads(){
        if (Parameters.numberOfPGA > 0){
            double[] times = new double[Parameters.numberOfAlgorithms];
            int counter = 0;
            for (PeriodicAlgorithm periodicAlgorithm : algorithms){
                times[counter] = periodicAlgorithm.getIterationTime();
                counter ++;
            }
            Arrays.sort(times);
            Parameters.timeLimitPerAlgorithm = (long) (times[ (int) Parameters.numberOfAlgorithms-1]);
            System.out.println("New time for single run is: " + Parameters.timeLimitPerAlgorithm);
        }
    }

    public void storeBestCurrentSolution() throws IOException, CloneNotSupportedException {
        for (PeriodicAlgorithm algorithm : algorithms){
            solutions.add(algorithm.storeSolution());
        }
        if (solutions.size() > 0){
            Collections.sort(solutions);
            PeriodicSolution bestSolution = solutions.get(0);
            if (!finalSolutions.contains(bestSolution)){
                finalSolutions.add(bestSolution);
            }
            Collections.sort(finalSolutions);
            SolutionStorer.store(finalSolutions.get(0), startTime, fileName);
        }
        solutions.clear();
    }

    public void generateOptimalSolution( ) throws CloneNotSupportedException {
        try{
            ArrayList<Journey>[][] journeys = getJourneys();
            //ArrayList<Journey>[][] otherJourneys = bestIterationSolution.getJourneys();
            //MIPTest.testJourneySimilarity(otherJourneys, journeys, data);
            if (journeyCombinationModel.runModel(journeys) == 2) {
                journeys = journeyCombinationModel.getOptimalJourneys();
                orderDistributionJCM = journeyCombinationModel.getOrderDistribution();
                System.out.println("OD valid? " + IndividualTest.testValidOrderDistribution(data, orderDistributionJCM));
                System.out.println("Fitness of od" + orderDistributionJCM.getFitness());
                PeriodicSolution JCMSolution = new JCMSolution(orderDistributionJCM.clone(), journeys);
                //SolutionStorer.store(JCMSolution, startTime, fileName);
                double improvement = (bestIterationFitness-JCMSolution.getFitness())/bestIterationFitness*100;
                System.out.println("Improvement from " + bestIterationFitness + " to " + JCMSolution.getFitness() + " equivalent to " + improvement + " %");
                if (bestIterationFitness < JCMSolution.getFitness()){
                    System.out.println("HYBRID MODEL MADE A WORSE SOLUTION");
                }
                double[] fitnesses = JCMSolution.getFitnesses();
                System.out.print(" | Time warp "+ fitnesses[1] + " | ");
                System.out.println("Over load "+ fitnesses[2]);
                int[] tags = Utils.getJourneyTags(JCMSolution.getJourneys(), data);
                //SolutionTest.checkForInfeasibility(JCMSolution, data);
                SolutionStorer.storeJBM(JCMSolution.getFitness(), journeyCombinationModel.runTime, improvement, tags[0], tags[1], journeyCombinationModel.optimal,startTime, fileName);
                solutions.add(JCMSolution);
                finalSolutions.add(JCMSolution);
            } else {
                orderDistributionJCM = pod.diversify(Parameters.diversifiedODsGenerated);
            }
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    private double getCurrentBestFitness() throws Exception {
        double fitness = 0;
        if (Parameters.useJCM && journeyCombinationModel.optimstatus == 2){  // TODO: 25/05/2020 Change this, as it needs to print the best solution 
            fitness = new JCMSolution(orderDistributionJCM.clone(), journeyCombinationModel.getOptimalJourneys()).getFitness();
        }
        else {
            try {
                OptionalDouble f = algorithms.stream().map(o -> {
                    try {
                        return o.storeSolution();
                    } catch (CloneNotSupportedException e) {
                        e.printStackTrace();
                    }
                    return null;
                }).filter(Objects::nonNull).mapToDouble(PeriodicSolution::getFitness).min();
                fitness = f.orElse(-1);
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        return fitness;
    }

    private void updateItertionsWithoutImprovement() throws Exception {
        double currentFitness = getCurrentBestFitness();
        if (currentFitness < this.currentBestSolution){
            this.currentBestSolution = currentFitness;
            iterationsWithoutImprovement = 0;
        }
        else{
            iterationsWithoutImprovement++;
        }
    }

    private void runIteration() throws Exception {
        //release all period swarms for
        downstreamGate.await();
        downstreamGate.reset();

        //wait for all periods to finish their generations
        upstreamGate.await();
        upstreamGate.reset();

        //update and find best order distribution
        PeriodicSolution solution;
        bestIterationFitness = Double.MAX_VALUE;
        bestIterationSolution = null;
        for (int s = 0 ; s < Parameters.numberOfAlgorithms ; s++){
            pod.distributions.set(s, algorithms.get(s).getOrderDistribution());
            solution = algorithms.get(s).storeSolution();
            if (solution.getFitness() < bestIterationFitness){
                bestIterationFitness = solution.getFitness();
                bestIterationSolution = solution;
            }
            System.out.println("Algorithm " + s + " fitness: "+ solution.getFitness() + " feasible: " + solution.isFeasible() + " infeasibility cost: " + solution.getInfeasibilityCost());
        }

    }

    public void updateOrderDistributionPopulation() throws CloneNotSupportedException {
        ArrayList<PeriodicSolution> solutions = new ArrayList<>();
        ArrayList<PeriodicAlgorithm> validAlgorithms = new ArrayList<>();
        for (PeriodicAlgorithm algorithm : algorithms){
            algorithm.setMinimumIterations(algorithm.getMinimumIterations() + 1);
            if (algorithm.getMinimumIterations() > Parameters.minimumIterationsPerOD){
                validAlgorithms.add(algorithm);
            }
        }
        for (PeriodicAlgorithm algorithm : validAlgorithms){
            solutions.add(algorithm.storeSolution());
        }
        int[] sortedIndices = IntStream.range(0, solutions.size())
                .boxed()
                .sorted(Comparator.comparing(solutions::get))
                .mapToInt(i -> i)
                .toArray();
        boolean firstOD = true;
        if (sortedIndices.length > 0){
            for (int i = sortedIndices.length - 1 ; i > Math.max(-1, sortedIndices.length - Parameters.orderDistributionCutoff -1) ; i--){
                System.out.println("changing od: " + validAlgorithms.get(sortedIndices[i]).getAlgorithmNumber());
                if (Parameters.useJCM && firstOD){
                    pod.distributions.set(validAlgorithms.get(i).getAlgorithmNumber(), orderDistributionJCM);
                    firstOD = false;
                }
                else{
                    pod.distributions.set(validAlgorithms.get(i).getAlgorithmNumber(), pod.diversify(Parameters.diversifiedODsGenerated));
                }

                //diversified new OD:
                validAlgorithms.get(sortedIndices[i]).updateOrderDistribution(pod.distributions.get(sortedIndices[i]));
                validAlgorithms.get(sortedIndices[i]).setMinimumIterations(0);
            }
        }

        for (int i = 0; i < Parameters.numberOfPGA ; i++){
            if (algorithms.get(i).getIterationsWithoutImprovement() > Parameters.PHGAIterationsWithoutImprovementLimit){
                System.out.println("MAX ITERATIONS HIT");
                finalSolutions.add(algorithms.get(i).storeSolution());
                pod.distributions.set(i, pod.diversify(Parameters.diversifiedODsGenerated));
                algorithms.get(i).updateOrderDistribution(pod.distributions.get(i));
            }
        }
        for (int i = Parameters.numberOfPGA; i < Parameters.numberOfAlgorithms ; i++){
            if (algorithms.get(i).getIterationsWithoutImprovement() > Parameters.ABCIterationsWithoutImprovementLimit){
                System.out.println("MAX ITERATIONS HIT");
                finalSolutions.add(algorithms.get(i).storeSolution());
                pod.distributions.set(i, pod.diversify(Parameters.diversifiedODsGenerated));
                algorithms.get(i).updateOrderDistribution(pod.distributions.get(i));
            }
        }
    }

    public ArrayList<Journey>[][] getJourneys() {
        ArrayList<Journey>[][] journeys = new ArrayList[data.numberOfPeriods][data.numberOfVehicleTypes];
        for (int p = 0 ; p < data.numberOfPeriods ; p++){
            for (int vt = 0 ; vt < data.numberOfVehicleTypes ; vt++){
                journeys[p][vt] = new ArrayList<>();
            }
        }
        ArrayList<Journey>[][] tempJourneys;
        for (PeriodicAlgorithm algorithm : algorithms){
            tempJourneys = algorithm.getJourneys();
            if (true)  //display journey number from each algorithm
                HybridTest.printNumberOfJourneys(tempJourneys,data);
            for (int p = 0 ; p < data.numberOfPeriods ; p++){
                for (int vt = 0 ; vt < data.numberOfVehicleTypes ; vt++){
                    for (Journey j : tempJourneys[p][vt]){
                        if (!journeys[p][vt].contains(j)) {
                            journeys[p][vt].add(j);
                        }
                    }
                }
            }
        }
        return journeys;
    }

    public static void main(String[] args) throws Exception {
        HybridController hc = new HybridController();
        hc.run();
        hc.terminate();
        System.out.println("Terminate");
    }

}
