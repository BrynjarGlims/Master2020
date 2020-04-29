package Master2020.ABC;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.DataFiles.Parameters;
import Master2020.Genetic.FitnessCalculation;
import Master2020.Individual.AdSplit;
import Master2020.Individual.Individual;
import Master2020.Individual.Journey;
import Master2020.MIP.OrderAllocationModel;
import Master2020.ProductAllocation.OrderDistribution;
import Master2020.StoringResults.Result;
import gurobi.GRBException;

import java.io.IOException;
import java.util.concurrent.BrokenBarrierException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;

import static Master2020.Testing.IndividualTest.testValidOrderDistribution;
import static java.lang.Thread.sleep;

public class Swarm {


    public Data data;
    public OrderDistribution orderDistribution;
    private OrderAllocationModel orderAllocationModel;
    public ArrayList<Journey>[][] journeys;

    public Swarm(Data data) throws GRBException {
        this.data = data;
        orderAllocationModel = new OrderAllocationModel(data);
        orderDistribution = new OrderDistribution(data);
        orderDistribution.makeInitialDistribution();
    }


    public void run() throws InterruptedException, BrokenBarrierException, IOException {
        //create
        CyclicBarrier downstreamGate = new CyclicBarrier(data.numberOfPeriods + 1);
        CyclicBarrier upstreamGate = new CyclicBarrier(data.numberOfPeriods + 1);
        List<PeriodSwarm> threads = makeThreadsAndStart(downstreamGate, upstreamGate);
        System.out.println("RUNNING FOR SAMPLE: " + Parameters.randomSeedValue);
        System.out.println("initial od valid?: " + testValidOrderDistribution(data, orderDistribution));

        for (int i = 0 ; i < Parameters.orderDistributionUpdates ; i++){
            System.out.println("GENERATION: " + i);
            //release all period swarms for
            downstreamGate.await();
            downstreamGate.reset();

            //wait for all periods to finish their generations
            upstreamGate.await();
            upstreamGate.reset();

            //update and find best order distribution
            double[] oldFitnesses = makeOptimalOrderDistribution(threads);
            double old = 0;
            for (double d : oldFitnesses){
                old += d;
            }
            updateOrderDistributionForColonies(threads);
            System.out.println("valid OD: " + testValidOrderDistribution(data, orderDistribution));
            double Prefitnesses = orderDistribution.getFitness();

            for (PeriodSwarm swarm : threads){
                Prefitnesses += swarm.globalBestFitness;
            }
            double[] fitnesses;
            double fitness;
            boolean feasible;
            double infeasibility;


            fitnesses = FitnessCalculation.getIndividualFitness(data, journeys, orderDistribution, 1);
            fitness =  orderDistribution.getFitness();
            for (double f : fitnesses){
                fitness+= f;
            }

            feasible = fitnesses[1] == 0 && fitnesses[2] == 0;
            infeasibility = fitnesses[1] + fitnesses[2];
            System.out.println("fitness: " + fitness + " old fitness: " + Prefitnesses + " feasible: " + feasible + " cost: " + infeasibility + " time warp: " + fitnesses[1] + " overload: " + fitnesses[2] + " vehicle cost: " + fitnesses[3]);

//            System.out.println("all customers exists: " + ABCtests.allCustomersExists(journeys, data));

        }
        //terminate threads
        for (PeriodSwarm periodSwarm : threads){
            periodSwarm.run = false;
        }
        downstreamGate.await();
        upstreamGate.await();
        Individual individual = HelperFunctions.createIndividual(data, journeys, orderDistribution);
        Result result = new Result(individual, "ABC");
        result.store();
    }

    private double[] makeOptimalOrderDistribution(List<PeriodSwarm> periodSwarms){
        PeriodSwarm periodSwarm;
        journeys = new ArrayList[data.numberOfPeriods][data.numberOfVehicleTypes];
        for (int p = 0 ; p < data.numberOfPeriods ; p++){
            periodSwarm = periodSwarms.get(p);
            ArrayList<Integer>[] giantTourEntry = HelperFunctions.parsePosition(data, p, periodSwarm.globalBestPosition);

            for (int vt = 0 ; vt < giantTourEntry.length ; vt++) {
                ArrayList<Journey> journeysEntry = AdSplit.adSplitSingular(giantTourEntry[vt], data, orderDistribution, p, vt);
                journeys[p][vt] = journeysEntry;
            }
        }
        double[] fitnesses = FitnessCalculation.getIndividualFitness(data, journeys, orderDistribution, 1);

        this.orderDistribution = orderAllocationModel.createOptimalOrderDistribution(journeys);
        return fitnesses;

    }

    private void updateOrderDistributionForColonies(List<PeriodSwarm> periodSwarms){
        for (PeriodSwarm periodSwarm : periodSwarms){
            periodSwarm.orderDistribution = this.orderDistribution;
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

    public static void main(String[] args) throws InterruptedException, BrokenBarrierException, GRBException, IOException {
        Data data = DataReader.loadData();
        Swarm swarm = new Swarm(data);
        swarm.run();
    }

}
