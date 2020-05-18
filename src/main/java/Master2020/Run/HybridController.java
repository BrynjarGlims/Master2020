package Master2020.Run;

import Master2020.ABC.PeriodicABC;
import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.DataFiles.Parameters;
import Master2020.Individual.Journey;
import Master2020.Interfaces.PeriodicAlgorithm;
import Master2020.Interfaces.PeriodicSolution;
import Master2020.MIP.JourneyCombinationModel;
import Master2020.PGA.GeneticPeriodicAlgorithm;
import Master2020.PGA.PGASolution;
import Master2020.Population.PeriodicOrderDistributionPopulation;
import Master2020.ProductAllocation.OrderDistribution;
import Master2020.StoringResults.SolutionStorer;
import gurobi.GRBException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.IntStream;

public class HybridController {



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
    public double time;





    public HybridController() throws GRBException {
        this.data = DataReader.loadData();
        initialize();
    }

    public void initialize() throws GRBException {
        time = System.currentTimeMillis();
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
        journeyCombinationModel.terminateModel();
    }

    public void run() throws Exception {

        for (int i = 0 ; i < Parameters.orderDistributionUpdates - 1 ; i++){
            runIteration();
            if (Parameters.useJCM)
                generateOptimalSolution();
            storeBestCurrentSolution();
            updateOrderDistributionPopulation();
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
        System.out.println(finalSolutions.get(0).getFitness());
        finalSolutions.get(0).writeSolution(fileName);
    }

    public void storeBestCurrentSolution() throws IOException {
        Collections.sort(solutions);
        SolutionStorer.store(solutions.get(solutions.size()-1), time, fileName);
    }

    public void generateOptimalSolution( ){
        ArrayList<Journey>[][] journeys = getJourneys();
        if (journeyCombinationModel.runModel(journeys) == 2) {
            journeys = journeyCombinationModel.getJourneys();
            orderDistributionJCM = journeyCombinationModel.getOrderDistribution();
            PeriodicSolution JCMSolution = new PGASolution(orderDistributionJCM, journeys);
            solutions.add(JCMSolution);
        } else {
            orderDistributionJCM = pod.diversify(3);
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
        for (int s = 0 ; s < Parameters.numberOfAlgorithms ; s++){
            algorithms.get(s).runIteration();
            pod.distributions.set(s, algorithms.get(s).getOrderDistribution());
            solution = algorithms.get(s).storeSolution();
            System.out.println("Algorithm " + s + " fitness: "+ solution.getFitness() + " feasible: " + solution.isFeasible() + " infeasibility cost: " + solution.getInfeasibilityCost());
        }
    }

    public void updateOrderDistributionPopulation() throws CloneNotSupportedException {
        solutions.clear();
        for (PeriodicAlgorithm algorithm : algorithms){
            algorithm.setMinimumIterations(algorithm.getMinimumIterations() + 1);
            solutions.add(algorithm.storeSolution());
        }
        int[] sortedIndices = IntStream.range(0, solutions.size())
                .boxed()
                .sorted(Comparator.comparing(i -> solutions.get(i)))
                .mapToInt(i -> i)
                .toArray();
        for (int i = sortedIndices.length - 1 ; i > sortedIndices.length - Parameters.orderDistributionCutoff ; i--){
            if (algorithms.get(sortedIndices[i]).getMinimumIterations() > Parameters.minimumIterations){
                System.out.println("changing od: " + sortedIndices[i]);

//                //random OD:
//                OrderDistribution newOD = new OrderDistribution(data);
//                newOD.makeInitialDistribution();
//                pod.distributions.set(sortedIndices[i], newOD);

                //diversified new OD:
                pod.distributions.set(sortedIndices[i], pod.diversify(3));
                algorithms.get(sortedIndices[i]).updateOrderDistribution(pod.distributions.get(sortedIndices[i]));
                algorithms.get(sortedIndices[i]).setMinimumIterations(0);
            }
        }
        for (int i = 0; i < algorithms.size() ; i++){
            if (algorithms.get(i).getIterationsWithoutImprovement() > Parameters.hybridIterationsWithoutImprovementLimit){
                System.out.println("MAX ITERATIONS HIT");
                finalSolutions.add(algorithms.get(i).storeSolution());
                pod.distributions.set(i, pod.diversify(3));
                algorithms.get(i).updateOrderDistribution(pod.distributions.get(i));
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
        for (PeriodicAlgorithm algorithm : algorithms){
            swarmJourneys = algorithm.getJourneys();
            for (int p = 0 ; p < data.numberOfPeriods ; p++){
                for (int vt = 0 ; vt < data.numberOfVehicleTypes ; vt++){
                    journeys[p][vt].addAll(swarmJourneys[p][vt]);
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
