package Individual;
import DataFiles.*;
import Population.Population;
import ProductAllocation.OrderDistribution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


public class Individual {
    //chromosomes
    public GiantTour giantTour;  //period, vehicleType
    public VehicleAssigment vehicleAssigment;
    public GiantTourSplit giantTourSplit;
    public OrderDistribution orderDistribution;

    public Data data;
    public boolean validCapacity;

    public double infeasibilityOvertimeValue;
    public double infeasibilityTimeWarpValue;
    public double infeasibilityOverCapacityValue;

    public Label[][] bestLabels;

    //// TODO: 18.02.2020 TO be removed
    public Population Population;

    public Individual(Data data, OrderDistribution orderDistribution) {
        this.data = data;

        this.infeasibilityOverCapacityValue = 0;
        this.infeasibilityOvertimeValue = 0;
        this.infeasibilityTimeWarpValue = 0;

        //set chromosome
        this.vehicleAssigment = new VehicleAssigment(data);
        this.giantTourSplit = new GiantTourSplit(data);
        this.giantTour = new GiantTour(data);
        this.orderDistribution = orderDistribution;

        this.bestLabels =new Label[data.numberOfPeriods][data.numberOfVehicleTypes];

    }

    public boolean isFeasible() {
        return (infeasibilityOverCapacityValue == 0 && infeasibilityOvertimeValue == 0
                && infeasibilityTimeWarpValue == 0);

    }
    public boolean hasValidTimeWindows() {
        //Todo: needs to be implemented
        return true;
    }

    public boolean hasValidVehicleCapacity() {
        //Todo: needs to be implemented
        return true;
    }

    public double evaluateIndividual() {
        //TODO: needs to be implemented
        return 0.0;
    }

    public int getRankOfIndividual() {
        int rank = 0; //TODO: implement rank calculations
        return rank;
    }

    public double getIndividualBiasedFitnessScore() {
        double fitness = 0.0; //TODO: implement fitness calculations
        //calculate biased fitness element
        int nbIndividuals = 0;
        if (this.isFeasible()) {
            nbIndividuals = Population.getSizeOfFeasiblePopulation();
        }
        else if (!this.isFeasible()) {
            nbIndividuals = Population.getSizeOfInfeasiblePopulation();
        }
        double biasedFitness = (1 - (Parameters.numberOfEliteIndividuals/nbIndividuals)*getRankOfIndividual());
        double fitnessScore = fitness + biasedFitness;
        return fitnessScore;
    }


    public static void main(String[] args){
        Data data = DataReader.loadData();
        OrderDistribution od = new OrderDistribution(data);
        od.makeDistribution();
        Individual individual = new Individual(data, od);
        AdSplit.adSplitPlural(individual);

        individual.giantTour.toString();
        individual.giantTourSplit.toString();
        individual.vehicleAssigment.toString();
    }


}








