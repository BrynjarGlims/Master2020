package Master2020.Interfaces;

import Master2020.Individual.Journey;
import Master2020.ProductAllocation.OrderDistribution;
import scala.util.parsing.combinator.testing.Str;

import java.io.IOException;
import java.util.ArrayList;

public interface PeriodicSolution extends Comparable<PeriodicSolution> {

    double getFitness();

    double[] getFitnesses();

    ArrayList<Journey>[][] getJourneys();

    OrderDistribution getOrderDistribution();

    boolean isFeasible();

    double getInfeasibilityCost();

    void writeSolution(String fileName, double startTime) throws IOException;
}
