package Master2020.PGA;

import Master2020.ABC.ABCSolution;
import Master2020.ABC.HelperFunctions;
import Master2020.DataFiles.Data;
import Master2020.DataFiles.Order;
import Master2020.DataFiles.Parameters;
import Master2020.Genetic.FitnessCalculation;
import Master2020.Individual.Individual;
import Master2020.Individual.Journey;
import Master2020.Interfaces.PeriodicSolution;
import Master2020.ProductAllocation.OrderDistribution;
import Master2020.StoringResults.Result;

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


    public PGASolution(OrderDistribution orderDistribution, ArrayList<Journey>[][] journeys){
        this.orderDistribution = orderDistribution;
        this.data = orderDistribution.data;
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

    public void printDetailedFitness(){
        double[] thisFitnesses = FitnessCalculation.getIndividualFitness(data, journeys, orderDistribution, 1, timeWarpPenalty, overLoadPenalty);
        System.out.println("Driving cost: " + thisFitnesses[0]);
        System.out.println("TimeWarp cost: " + thisFitnesses[1]);
        System.out.println("OverLoad cost: " + thisFitnesses[2]);
        System.out.println("Vehicle usage cost: " + thisFitnesses[3]);
        System.out.println("OrderDistributionCost: " + orderDistribution.getFitness());
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

    public double[] getFitnesses(){
        return FitnessCalculation.getIndividualFitness(data, journeys, orderDistribution, 1, timeWarpPenalty, overLoadPenalty);
    }

    @Override
    public void writeSolution(String fileName, double startTime) throws IOException {
        Individual individual = HelperFunctions.createIndividual(data, journeys, orderDistribution);
        Result result = new Result(individual, "PGA", fileName, individual.isFeasible() , false);
        System.out.println("old fitness: " + fitness + " new fitness to file: " + individual.getFitness(false));
        result.store(startTime);
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

    public String toString(){
        return " " + fitness;
    }

}
