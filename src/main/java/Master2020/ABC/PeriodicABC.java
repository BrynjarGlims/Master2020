package Master2020.ABC;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.DataFiles.Parameters;
import Master2020.Genetic.FitnessCalculation;
import Master2020.Individual.AdSplit;
import Master2020.Individual.Origin;
import Master2020.Individual.Journey;
import Master2020.Interfaces.PeriodicAlgorithm;
import Master2020.MIP.OrderAllocationModel;
import Master2020.Population.PeriodicOrderDistributionPopulation;
import Master2020.ProductAllocation.OrderDistribution;
import Master2020.Run.HybridController;
import Master2020.Testing.ABCtests;
import gurobi.GRBException;

import java.io.IOException;
import java.util.concurrent.BrokenBarrierException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;

public class PeriodicABC extends Thread implements PeriodicAlgorithm {


    public Data data;
    public OrderDistribution orderDistribution;
    private OrderAllocationModel orderAllocationModel;
    public ArrayList<Journey>[][] journeys;
    private CyclicBarrier downstreamGate;
    private CyclicBarrier upstreamGate;
    private CyclicBarrier masterUpstreamGate;
    private CyclicBarrier masterDownstreamGate;
    private List<PeriodSwarm> threads;
    private double fitness;
    public int iterationsWithoutImprovement;
    public int minimumIterations;
    public boolean run;
    public int algorithmNumber;

    public PeriodicABC(Data data) throws GRBException {
        this.data = data;
        algorithmNumber = HybridController.algorithmCounter;
        HybridController.algorithmCounter++;
        orderAllocationModel = new OrderAllocationModel(data);
        orderDistribution = new OrderDistribution(data);
        orderDistribution.makeInitialDistribution();
    }

    public void initialize(OrderDistribution orderDistribution, CyclicBarrier masterDownstreamGate, CyclicBarrier masterUpstreamGate){
        initialize(orderDistribution);
        this.masterDownstreamGate = masterDownstreamGate;
        this.masterUpstreamGate = masterUpstreamGate;
        this.run = true;
        minimumIterations = 0;
    }

    public void initialize(OrderDistribution orderDistribution){
        this.orderDistribution = orderDistribution;
        iterationsWithoutImprovement = 0;
        downstreamGate = new CyclicBarrier(data.numberOfPeriods + 1);
        upstreamGate = new CyclicBarrier(data.numberOfPeriods + 1);
        threads = makeThreadsAndStart(downstreamGate, upstreamGate);
    }


    public void run(){
        while (run){
            try {
                //wait for all threads to be ready
                masterDownstreamGate.await();
                //run generations
                long startTime = System.currentTimeMillis();
                if(run){
                    while(System.currentTimeMillis() - startTime < Parameters.timeLimitPerAlgorithm){
                        if (System.currentTimeMillis() - startTime > Parameters.totalRuntime){
                            break;
                        }
                        {runIteration();}
                    }
                    updateFitness();
                }
                //wait for all periods to finish
                masterUpstreamGate.await();

            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
        }
    }

    public void updateOrderDistribution(OrderDistribution orderDistribution){
        this.orderDistribution = orderDistribution;
        updateOrderDistributionForColonies(threads,true);
        iterationsWithoutImprovement = 0;
    }

    public double getIterationTime(){
        return 0;  //set to zero as there is no measure of this in ABC
    }



    public void runIteration() throws InterruptedException, BrokenBarrierException {
        //release all period swarms for
        downstreamGate.await();
        downstreamGate.reset();

        //wait for all periods to finish their generations
        upstreamGate.await();
        upstreamGate.reset();

        //update and find best order distribution
        makeOptimalOrderDistribution(threads);
        updateOrderDistributionForColonies(threads, false);


    }


    private void updateFitness(){
        double[] fitnesses = FitnessCalculation.getIndividualFitness(data, journeys, orderDistribution,
                1, Parameters.initialTimeWarpPenalty, Parameters.initialOverLoadPenalty);
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

    public void terminate() throws BrokenBarrierException, InterruptedException {
        //terminate threads
        for (PeriodSwarm periodSwarm : threads){
            periodSwarm.run = false;
        }
        downstreamGate.await();
        upstreamGate.await();
//        ABCSolution solution = storeSolution();

    }


    private OrderDistribution makeOptimalOrderDistribution(double[][] positions){
        journeys = new ArrayList[data.numberOfPeriods][data.numberOfVehicleTypes];
        for (int p = 0 ; p < data.numberOfPeriods ; p++){
            ArrayList<Integer>[] giantTourEntry = HelperFunctions.parsePosition(data, p, positions[p]);

            for (int vt = 0 ; vt < giantTourEntry.length ; vt++) {
                ArrayList<Journey> journeysEntry = AdSplit.adSplitSingular(giantTourEntry[vt], data, orderDistribution, p, vt, threads.get(p).penaltyControl.timeWarpPenalty, threads.get(p).penaltyControl.overLoadPenalty);
                journeys[p][vt] = journeysEntry;
            }
        }
        if (orderAllocationModel.createOptimalOrderDistribution(journeys, 1) == 2){
            return orderAllocationModel.getOrderDistribution();
        }
        else{
            System.out.println("no optimal OD found");
            return this.orderDistribution;
        }
    }

    private void makeOptimalOrderDistribution(List<PeriodSwarm> periodSwarms){
        PeriodSwarm periodSwarm;
        journeys = new ArrayList[data.numberOfPeriods][data.numberOfVehicleTypes];
        for (int p = 0 ; p < data.numberOfPeriods ; p++){
            periodSwarm = periodSwarms.get(p);
            ArrayList<Integer>[] giantTourEntry = HelperFunctions.parsePosition(data, p, periodSwarm.globalBestPosition);

            for (int vt = 0 ; vt < giantTourEntry.length ; vt++) {
                ArrayList<Journey> journeysEntry = AdSplit.adSplitSingular(giantTourEntry[vt], data, orderDistribution, p, vt, periodSwarm.penaltyControl.timeWarpPenalty, periodSwarm.penaltyControl.overLoadPenalty);
                journeys[p][vt] = journeysEntry;
            }
        }
        if (ABCtests.allCustomersExists(journeys, data) && orderAllocationModel.createOptimalOrderDistribution(journeys, 1) == 2){
            this.orderDistribution = orderAllocationModel.getOrderDistribution();
        }
        else{
            System.out.println("no optimal OD found");
        }

    }

    private void updateOrderDistributionForColonies(List<PeriodSwarm> periodSwarms, boolean newOD){
        for (PeriodSwarm periodSwarm : periodSwarms){
            periodSwarm.orderDistribution = this.orderDistribution;
            if (newOD){
                periodSwarm.initialize();
            }
        }
    }

    private List<PeriodSwarm> makeThreadsAndStart(CyclicBarrier downstreamGate, CyclicBarrier upstreamGate){
        List<PeriodSwarm> threads = new ArrayList<>();
        for (int p = 0 ; p < data.numberOfPeriods ; p++){
            PeriodSwarm periodSwarm = new PeriodSwarm(data, p, orderDistribution, downstreamGate, upstreamGate);
            threads.add(periodSwarm);
        }
        for (Thread t : threads){
            t.start();
        }
        return threads;
    }

    public ArrayList<Journey>[][] getJourneys(){
        ArrayList<Journey>[][] journeys = new ArrayList[data.numberOfPeriods][data.numberOfVehicleTypes];
        for (int p = 0 ; p < data.numberOfPeriods ; p++){
            for (int vt = 0 ; vt < data.numberOfVehicleTypes ; vt++){
                journeys[p][vt] = new ArrayList<>();
            }
        }
        for (PeriodSwarm periodSwarm : threads){
            for (ABCPeriodSolution solution : periodSwarm.solutions){
                for (int vt = 0 ; vt < data.numberOfVehicleTypes ; vt++){
                    for (Journey journey : solution.journeys[vt]){
                        journey.ID = Origin.ABC;   //set identificator of PGA
                    }
                    journeys[periodSwarm.period][vt].addAll(solution.journeys[vt]);
                }
            }
        }
        return journeys;
    }

    public ABCSolution storeSolution() {
        double[][] positions = new double[threads.size()][];
        for (PeriodSwarm ps : threads){
            positions[ps.period] = ps.globalBestPosition.clone();
        }
        return new ABCSolution(positions, makeOptimalOrderDistribution(positions), journeys);
    }

    public OrderDistribution getOrderDistribution(){
        return orderDistribution;
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

    public int getAlgorithmNumber() {return algorithmNumber;}

    public static void main(String[] args) throws InterruptedException, BrokenBarrierException, GRBException, IOException, CloneNotSupportedException {
        Data data = DataReader.loadData();
        PeriodicABC swarm = new PeriodicABC(data);
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

        swarm.run();
    }
}
