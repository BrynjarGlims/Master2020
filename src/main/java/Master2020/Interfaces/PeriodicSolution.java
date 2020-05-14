package Master2020.Interfaces;

import Master2020.Individual.Journey;
import Master2020.ProductAllocation.OrderDistribution;

import java.io.IOException;
import java.util.ArrayList;

public interface PeriodicSolution extends Comparable<PeriodicSolution> {

    double getFitness();

    ArrayList<Journey>[][] getJourneys();

    OrderDistribution getOrderDistribution();

    boolean isFeasible();

    double getInfeasibilityCost();

    void writeSolution() throws IOException;
}
