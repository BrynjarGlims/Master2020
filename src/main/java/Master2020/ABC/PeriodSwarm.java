package Master2020.ABC;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.DataFiles.Parameters;

import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PeriodSwarm extends Thread {

    public Data data;
    public int period;
    public Set<Bee> bees;
    public CyclicBarrier downstreamGate;
    public CyclicBarrier upstreamGate;

    public int sum = 0;


    public PeriodSwarm(Data data, int period, CyclicBarrier downstreamGate, CyclicBarrier upstreamGate){
        this.data = data;
        this.period = period;
        initialize();
        this.downstreamGate = downstreamGate;
        this.upstreamGate = upstreamGate;

    }


    @Override
    public void run() {
        try {
            System.out.println("waiting for release");
            downstreamGate.await();
            System.out.println("released thread: " + getName());
            int time = ThreadLocalRandom.current().nextInt(1000, 5000);
            System.out.println("waiting for " + time + " milliseconds on thread " + getName());
            sleep(time);
            upstreamGate.await();
            downstreamGate.await();
            System.out.println("finished thread: " + getName());
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
    }



    public void initialize(){
        bees = IntStream.range(0, Parameters.numberOfBees).parallel().mapToObj(o -> new Bee(data, period)).collect(Collectors.toSet());
    }


    public static void main(String[] args){
        Data data = DataReader.loadData();
    }



}
