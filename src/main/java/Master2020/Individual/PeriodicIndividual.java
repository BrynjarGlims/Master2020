package Master2020.Individual;

import Master2020.DataFiles.Data;
import Master2020.Genetic.FitnessCalculation;
import Master2020.Population.PeriodicPopulation;
import Master2020.ProductAllocation.OrderDistribution;
import Master2020.Testing.IndividualTest;
import gurobi.GRB;
import java.util.ArrayList;

public class PeriodicIndividual {

    public Data data;
    public Individual[] individuals;
    public double fitness = Double.MAX_VALUE;
    public OrderDistribution orderDistribution;

    public double infeasibilityCost= Double.MAX_VALUE;
    public double travelCost;
    public double vehicleUsageCost;
    public double timeWarpCost;
    public double overLoadCost;
    public double orderDistributionCost;

    private double diversity = -1;
    private double biasedFitness;
    private ArrayList<Journey>[][] journeys;


    public PeriodicIndividual(Data data){
        this.data = data;
        this.journeys = new ArrayList[data.numberOfPeriods][data.numberOfVehicleTypes];
        for (int p  = 0; p < data.numberOfPeriods; p++ ){
            for (int vt = 0; vt < data.numberOfVehicleTypes; vt++){
                this.journeys[p][vt] = new ArrayList<Journey>();
            }
        }
        this.individuals = new Individual[data.numberOfPeriods];
    }

    public void setPeriodicIndividual(Individual individual, int p){

        this.individuals[p] = individual;
        initializeJourneys(p);
    }

    public ArrayList<Journey>[][] getJourneys() {
        return journeys;
    }

    public void setOrderDistribution(OrderDistribution orderDistribution){
        this.orderDistribution = orderDistribution;
    }



    public Individual getPeriodicIndividual(int p){
        if (this.individuals[p] == null){
            System.out.println("Individual not set");
            return null;
        }
        else{
            return this.individuals[p];
        }
    }

    public void printDetailedInformation(){
        System.out.println("-------------------------------------");
        System.out.println("Periodic Individual - " + this.hashCode());
        System.out.println("Fitness: " + fitness);
        System.out.println("Is feasbile: " + isFeasible());
        System.out.println("Infeasibility cost: " + infeasibilityCost);
        System.out.println(" #Detailed cost# ");
        System.out.println("Travel cost: " + travelCost);
        System.out.println("Vehicle usage cost: " + vehicleUsageCost);
        System.out.println("Order allocation cost: " + orderDistributionCost);
        System.out.println("Time warp cost: " + timeWarpCost);
        System.out.println("Over load cost: " + overLoadCost);
        System.out.println("-------------------------------------");
    }


    public void resetCosts(){
        this.fitness = 0;
        this.travelCost = 0;
        this.infeasibilityCost = 0;
        this.vehicleUsageCost = 0;
        this.timeWarpCost = 0;
        this.overLoadCost = 0;
    }

    public boolean isFeasible(){
        return this.infeasibilityCost == 0;
    }

    public double getFitness() {
        double[] fitnesses =  FitnessCalculation.getIndividualFitness(data, journeys, orderDistribution, 1 );
        this.travelCost = fitnesses[0];
        this.timeWarpCost = fitnesses[1];
        this.overLoadCost = fitnesses[2];
        this.vehicleUsageCost = fitnesses[3];
        this.infeasibilityCost = fitnesses[1] + fitnesses[2];
        this.orderDistributionCost = orderDistribution.getFitness();
        this.fitness = this.travelCost + this.timeWarpCost + this.overLoadCost + this.vehicleUsageCost + this.orderDistributionCost;
        return fitness;
    }

    public void initializeJourneys(int p){
        for (int vt = 0; vt < data.numberOfVehicleTypes; vt++){
            this.journeys[p][vt] = this.individuals[p].journeyList[0][vt];
        }
    }

    public Individual createStandardIndividualObject(){
        Individual newIndividual = new Individual(this.data, null, false, -1);
        newIndividual.journeyList = this.journeys;
        return newIndividual;
    }

    public int compareTo(PeriodicIndividual periodicIndividual) { // TODO: 04.03.2020 Sort by biased fitness and not fitness
        return this.getFitness() < periodicIndividual.getFitness() ? 1 : -1;
    }


}
