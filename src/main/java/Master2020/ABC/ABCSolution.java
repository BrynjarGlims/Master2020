package Master2020.ABC;

import Master2020.DataFiles.Data;
import Master2020.Genetic.FitnessCalculation;
import Master2020.Individual.Individual;
import Master2020.Individual.Journey;
import Master2020.Interfaces.PeriodicSolution;
import Master2020.StoringResults.Result;
import Master2020.ProductAllocation.OrderDistribution;

import java.io.IOException;
import java.util.ArrayList;

public class ABCSolution implements PeriodicSolution {


    public double[][] positions;
    public OrderDistribution orderDistribution;
    public ArrayList<Journey>[][] journeys;
    public boolean feasible;
    public double fitness;
    public double infeasibilityCost;
    public Data data;


    public ABCSolution(double[][] positions, OrderDistribution orderDistribution, ArrayList<Journey>[][] journeys){
        this.positions = positions;
        this.orderDistribution = orderDistribution;
        this.data = orderDistribution.data;
        this.journeys = journeys;
    }

    public double getFitness(){
        double[] thisFitnesses = FitnessCalculation.getIndividualFitness(data, journeys, orderDistribution, 1);
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
    public int compareTo(PeriodicSolution o) {
        double[] thisFitnesses = FitnessCalculation.getIndividualFitness(data, journeys, orderDistribution, 1);
        double[] oFitnesses = FitnessCalculation.getIndividualFitness(data, o.getJourneys(), o.getOrderDistribution(), 1);
        double thisFitness = 0;
        double oFitness = 0;
        for (int d = 0 ; d < thisFitnesses.length ; d++){
            thisFitness += thisFitnesses[d];
            oFitness += oFitnesses[d];
        }
        if (thisFitness < oFitness){
            return -1;
        }
        else if (thisFitness > oFitness){
            return 1;
        }
        return 0;
    }

    public void writeSolution() throws IOException {
        Individual individual = HelperFunctions.createIndividual(data, journeys, orderDistribution);
        Result result = new Result(individual, "ABC");
        result.store();

    }

}
