package Master2020.ABC;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.DataFiles.Parameters;
import Master2020.ProductAllocation.OrderDistribution;
import Master2020.Utils.WeightedRandomSampler;

import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PeriodSwarm extends Thread {

    public Data data;
    public int period;
    public OrderDistribution orderDistribution;
    public List<Employee> employees;
    public List<Onlooker> onlookers;
    public CyclicBarrier downstreamGate;
    public CyclicBarrier upstreamGate;
    public double globalBestFitness;
    public double[] globalBestPosition;
    public boolean run = true;


    public PeriodSwarm(Data data, int period, OrderDistribution orderDistribution, CyclicBarrier downstreamGate, CyclicBarrier upstreamGate){
        this.data = data;
        this.period = period;
        this.downstreamGate = downstreamGate;
        this.upstreamGate = upstreamGate;
        this.orderDistribution = orderDistribution;
        initialize();
    }

    public PeriodSwarm(Data data, int period, OrderDistribution orderDistribution){
        this.data = data;
        this.period = period;
        this.orderDistribution = orderDistribution;
        initialize();
    }

    public void runGenerations(int generations){
        for (int i = 0 ; i < generations ; i++){
            runGeneration();
        }
    }


    public void runGeneration(){
        Bee neighbor;

        //Employee stage:
        for (Employee employee : employees){
            neighbor = getRandomNeighbor(employee);
            employee.search(neighbor);
        }
        double[] fitnesses = employees.stream().mapToDouble(o -> 1/o.fitness).toArray();
        WeightedRandomSampler weightedRandomSampler = new WeightedRandomSampler(fitnesses);

        //Onlooker stage:
        for (Onlooker onlooker : onlookers){
            Employee employee = employees.get(weightedRandomSampler.nextIndex());
            neighbor = getRandomNeighbor(onlooker);
            onlooker.search(neighbor, employee);
        }

        //update employee
        for (Employee employee : employees){
            employee.updateToBestPosition();
        }

        //scoute stage:
        for (Employee employee : employees){
            if (employee.trials > Parameters.maxNumberOfTrials){
                employee.scout();
            }
        }

    }


    public Bee getRandomNeighbor(Bee bee){
        Bee neighbor;
        int selected = ThreadLocalRandom.current().nextInt(0, (Parameters.numberOfOnlookers + Parameters.numberOfEmployees));
        if (selected < Parameters.numberOfEmployees){
            neighbor = employees.get(selected);
        }
        else{
            neighbor = onlookers.get(selected - Parameters.numberOfEmployees);
        }
        if (neighbor == bee){
            return getRandomNeighbor(bee);
        }
        return neighbor;
    }


    @Override
    public void run() {
        while (run){
            try {
                //wait for all threads to be ready
                downstreamGate.await();
                //run generations
                if (run){runGenerations(Parameters.generationsPerOrderDistribution);}
                //wait for all periods to finish
                upstreamGate.await();

            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
        }

    }



    public void initialize(){
        employees = IntStream.range(0, Parameters.numberOfEmployees).parallel().mapToObj(o -> new Employee(data, period, this)).collect(Collectors.toList());
        onlookers = IntStream.range(0, Parameters.numberOfOnlookers).parallel().mapToObj(o -> new Onlooker(data, period, this)).collect(Collectors.toList());
        globalBestFitness = Double.MAX_VALUE;
        globalBestPosition = employees.get(0).position;
    }


    public static void main(String[] args){
        Data data = DataReader.loadData();
        OrderDistribution orderDistribution = new OrderDistribution(data);
        orderDistribution.makeInitialDistribution();
        PeriodSwarm periodSwarm = new PeriodSwarm(data, 0, orderDistribution);
        periodSwarm.runGenerations(10000);

    }
}
