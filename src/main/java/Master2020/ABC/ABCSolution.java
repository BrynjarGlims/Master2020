package Master2020.ABC;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.Parameters;
import Master2020.Genetic.FitnessCalculation;
import Master2020.Individual.Individual;
import Master2020.Individual.Journey;
import Master2020.Interfaces.PeriodicSolution;
import Master2020.StoringResults.Result;
import Master2020.ProductAllocation.OrderDistribution;
import Master2020.Testing.IndividualTest;

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
    public double timeWarpPenalty;
    public double overLoadPenalty;


    public ABCSolution(double[][] positions, OrderDistribution orderDistribution, ArrayList<Journey>[][] journeys){
        this.positions = positions;
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

    public double[] getFitnesses(){
        return FitnessCalculation.getIndividualFitness(data, journeys, orderDistribution, 1, timeWarpPenalty, overLoadPenalty);
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
        double[] thisFitnesses = FitnessCalculation.getIndividualFitness(data, journeys, orderDistribution, 1, timeWarpPenalty, overLoadPenalty);
        double[] oFitnesses = FitnessCalculation.getIndividualFitness(data, o.getJourneys(), o.getOrderDistribution(), 1, timeWarpPenalty, overLoadPenalty);
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

    public void writeSolution(String fileName, double startTime) throws IOException {
        Individual individual = HelperFunctions.createIndividual(data, journeys, orderDistribution);
        System.out.println(IndividualTest.testValidOrderDistribution(data, orderDistribution));
        System.out.println(IndividualTest.checkIfIndividualIsComplete(individual));
        Result result = new Result(individual, "ABC", fileName, individual.isFeasible() , false);
        result.store(startTime);

    }

}
