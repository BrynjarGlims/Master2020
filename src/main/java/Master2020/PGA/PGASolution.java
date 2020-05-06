package Master2020.PGA;

import Master2020.ABC.ABCSolution;
import Master2020.DataFiles.Data;
import Master2020.DataFiles.Order;
import Master2020.Genetic.FitnessCalculation;
import Master2020.Individual.Individual;
import Master2020.Individual.Journey;
import Master2020.ProductAllocation.OrderDistribution;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.jar.JarEntry;

public class PGASolution implements Comparable<PGASolution> {

    Data data;
    OrderDistribution orderDistribution;
    ArrayList<Journey>[][] journeys;
    Individual individual;
    double fitness;

    public PGASolution(Data data){
        this.data = data;
    }

    public void initialize(ArrayList<Journey>[][] journeys, OrderDistribution orderDistribution){
        this.orderDistribution = orderDistribution;
        this.journeys = journeys;
    }

    public void setIndividual(Individual individual){
        this.individual = individual;
    }

    public int compareTo(PGASolution o) {
        double[] thisFitnesses = FitnessCalculation.getIndividualFitness(data, journeys, orderDistribution, 1);
        double[] oFitnesses = FitnessCalculation.getIndividualFitness(data, o.journeys, o.orderDistribution, 1);
        fitness = 0;
        double oFitness = 0;
        for (int d = 0 ; d < thisFitnesses.length ; d++){
            fitness += thisFitnesses[d];
            oFitness += oFitnesses[d];
        }

        if (fitness < oFitness){
            return -1;
        }
        else if (fitness > oFitness){
            return 1;
        }
        return 0;
    }

}
