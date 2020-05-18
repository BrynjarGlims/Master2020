package Master2020.Interfaces;

import Master2020.Individual.Journey;
import Master2020.ProductAllocation.OrderDistribution;
import gurobi.GRBException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public interface PeriodicAlgorithm {

    OrderDistribution getOrderDistribution();

    void initialize(OrderDistribution orderDistribution, CyclicBarrier masterDownstreamGate, CyclicBarrier masterUpstreamGate) throws GRBException;

    void run();

    void updateOrderDistribution(OrderDistribution orderDistribution);

    void runIteration() throws Exception;

    void terminate() throws BrokenBarrierException, InterruptedException, CloneNotSupportedException, IOException;

    ArrayList<Journey>[][] getJourneys();

    void setRun(boolean run);

    void setMinimumIterations(int value);

    int getMinimumIterations();

    int getIterationsWithoutImprovement();

    PeriodicSolution storeSolution() throws CloneNotSupportedException;

    double getIterationTime();
}
