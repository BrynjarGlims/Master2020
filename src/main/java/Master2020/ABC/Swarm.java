package Master2020.ABC;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.Individual.AdSplit;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static java.lang.Thread.sleep;

public class Swarm {


    Data data;
    public int[] finished;

    public Swarm(Data data){
        this.data = data;
    }


    public void run() throws InterruptedException, BrokenBarrierException {
        final CyclicBarrier downstreamGate = new CyclicBarrier(data.numberOfPeriods + 1);
        final CyclicBarrier upstreamGate = new CyclicBarrier(data.numberOfPeriods + 1);
        List<Thread> threads = new ArrayList<>();
        finished = new int[data.numberOfPeriods];
        for (int p = 0 ; p < data.numberOfPeriods ; p++){
            PeriodSwarm periodSwarm = new PeriodSwarm(data, p, downstreamGate, upstreamGate);
            threads.add(periodSwarm);
        }
        for (Thread t : threads){
            t.start();
        }
        sleep(3000);
        downstreamGate.await();
        downstreamGate.reset();
        System.out.println("here?");
        upstreamGate.await();
        System.out.println("main thread finished waiting for below threads");
        sleep(3000);
        downstreamGate.await();
        System.out.println("finished running");
    }













    public static void main(String[] args) throws InterruptedException, BrokenBarrierException {
        Data data = DataReader.loadData();
        Swarm swarm = new Swarm(data);
        swarm.run();
    }

}
