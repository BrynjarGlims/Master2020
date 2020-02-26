package Individual;
import DataFiles.*;
import Population.Population;
import ProductAllocation.OrderDistribution;
import scala.collection.parallel.mutable.ParHashMapCombiner;

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
    public Population population;

    public Data data;
    public boolean validCapacity;

    public double infeasibilityOvertimeValue;
    public double infeasibilityTimeWarpValue;
    public double infeasibilityOverCapacityValue;

    public Label[][] bestLabels;

    //// TODO: 18.02.2020 TO be removed
    public Population Population;


    //fitness values:
    public double objectiveCost;
    public double infeasibilityCost;

    public double fitness = Double.MAX_VALUE;
    public double diversity = 0;
    public double biasedFitness;



    public Individual(Data data, OrderDistribution orderDistribution, Population population) {
        this.data = data;
        this.orderDistribution = orderDistribution;
        this.population = population;


        this.infeasibilityOverCapacityValue = 0;
        this.infeasibilityOvertimeValue = 0;
        this.infeasibilityTimeWarpValue = 0;

        //set chromosome
        this.vehicleAssigment = new VehicleAssigment(data);
        this.giantTourSplit = new GiantTourSplit(data);
        this.giantTour = new GiantTour(data);

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

    public double getFitness(boolean update){
        if (update || this.fitness == Double.MAX_VALUE){
            updateFitness();
            return fitness;
        }
        else {
            return fitness;
        }
    }



    public void updateFitness(){
        double fitness = 0;

        //Calculate objective costs
        this.objectiveCost = getObjectiveCost();

        //Add infeasibility costs
        this.infeasibilityCost = getInfeasibilityCost();

        this.fitness = this.objectiveCost + this.infeasibilityCost;

    }

    private double getObjectiveCost(){
        objectiveCost = 0;
        for (Label[] labels : bestLabels){
            for (Label label : labels){
                if (label.isEmptyLabel){
                    continue;
                }
                //Adds driving cost
                objectiveCost += label.getLabelDrivingDistance() * data.vehicleTypes[label.vehicleTypeID].travelCost;
                //Adds vehicle use cost
                objectiveCost += label.getNumberOfVehicles() * data.vehicleTypes[label.vehicleTypeID].usageCost;

            }
        }
        objectiveCost += orderDistribution.getOvertimeValue();
        return objectiveCost;
    }

    private double getInfeasibilityCost(){
        double infeasibilityCost = 0;
        for (Label[] labels : bestLabels) {
            for (Label label : labels) {
                if (label.isEmptyLabel){
                    continue;
                }
                //Already added scaling parameters in label
                infeasibilityCost += label.getTimeWarpInfeasibility();
                infeasibilityCost += label.getLoadInfeasibility();
                infeasibilityCost += label.getOvertimeInfeasibility();
            }
        }
        return infeasibilityCost;
    }

    public double getIndividualBiasedFitnessScore() {
        fitness = 0.0; //TODO: implement fitness calculations
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

    public double calculateDiversity(Individual comparison){

        return 0;

    }

    public double hammingDistance(GiantTour gt){
        double customerDistance = 0;
        double vehicleTypeDistance = 0;
        int vt1 = 0;
        int vt2 = 0;
        int counter1 = 0;
        int counter2 = 0;
        for (int p = 0; p < data.numberOfPeriods; p++){
            for (int c = 0; c < data.numberOfCustomerVisitsInPeriod[p]; c++) {
                if (gt.chromosome[p][vt2].size() == counter2) {
                    counter2 = 0;
                    vt2++;
                }
                if (this.giantTour.chromosome[p][vt1].size() - 1 == counter1) {
                    counter1 = 0;
                    vt1++;
                }
                customerDistance += (this.giantTour.chromosome[p][vt1].get(counter1) != gt.chromosome[p][vt2].get(counter2)) ? 1 : 0;
                vehicleTypeDistance += (vt2 != vt1) ? 1 : 0;
                counter1++;
                counter2++;
            }
            
        }
        customerDistance /= 2*data.numberOfCustomerVisitsInPlanningHorizon;

        // TODO: 26.02.2020 Check if the proportional customer distance and vehicle type distance
        return customerDistance + vehicleTypeDistance; //larger distance, more diversity
    }




    public static void main(String[] args){
        Individual individual = Individual.makeIndividual();
        System.out.println("Value of fitness: " + individual.getFitness(false));
    }

    public static Individual makeIndividual(){
        Data data = DataReader.loadData();
        OrderDistribution od = new OrderDistribution(data);
        od.makeDistribution();
        Individual individual = new Individual(data, od, null);
        AdSplit.adSplitPlural(individual);
        return individual;
    }



}









