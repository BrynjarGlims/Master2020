package Master2020.PGA;

import Master2020.ABC.ABCSolution;
import Master2020.DataFiles.Data;
import Master2020.DataFiles.Order;
import Master2020.DataFiles.Parameters;
import Master2020.Genetic.FitnessCalculation;
import Master2020.Individual.Individual;
import Master2020.Individual.Journey;
import Master2020.Interfaces.PeriodicSolution;
import Master2020.ProductAllocation.OrderDistribution;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.jar.JarEntry;

public class PGASolution implements PeriodicSolution {

    Data data;
    OrderDistribution orderDistribution;
    ArrayList<Journey>[][] journeys;
    Individual individual;
    double fitness;
    public boolean feasible;
    public double infeasibilityCost;

    public double timeWarpPenalty;
    public double overLoadPenalty;

    public PGASolution(Data data){
        this.data = data;
    }

    public void initialize(ArrayList<Journey>[][] journeys, OrderDistribution orderDistribution, double timeWarpPenalty, double overLoadPenalty){
        this.orderDistribution = orderDistribution;
        this.journeys = journeys;
        this.timeWarpPenalty = Parameters.initialTimeWarpPenalty;
        this.overLoadPenalty = Parameters.initialOverLoadPenalty;
    }

    public double getFitness(){
        double[] thisFitnesses = FitnessCalculation.getIndividualFitness(data, journeys, orderDistribution, 1, timeWarpPenalty, overLoadPenalty);
        double fitness = 0;
        for (int d = 0 ; d < thisFitnesses.length ; d++){
            fitness += thisFitnesses[d];
        }
        feasible = thisFitnesses[1] == 0 && thisFitnesses[2] == 0;
        this.fitness = fitness;
        this.infeasibilityCost = thisFitnesses[1] + thisFitnesses[2];
        return fitness;
    }

    public ArrayList<Journey>[][] getJourneys(){
        return journeys;
    }

    public OrderDistribution getOrderDistribution(){
        return orderDistribution;
    }

    public boolean isFeasible(){
        return feasible;
    }

    public double getInfeasibilityCost(){
        return infeasibilityCost;
    }

    @Override
    public void writeSolution() throws IOException {
        // TODO: 14.05.2020 make this @Kåre
    }

    public void setIndividual(Individual individual){
        this.individual = individual;
    }

    public int compareTo(PeriodicSolution o) {
        double[] thisFitnesses = FitnessCalculation.getIndividualFitness(data, journeys, orderDistribution, 1, timeWarpPenalty, overLoadPenalty);
        double[] oFitnesses = FitnessCalculation.getIndividualFitness(data, o.getJourneys(), o.getOrderDistribution(), 1,timeWarpPenalty, overLoadPenalty);
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
