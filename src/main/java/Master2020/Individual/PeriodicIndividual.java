package Master2020.Individual;

import Master2020.DataFiles.Data;
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
    }

    public void setPeriodicIndividual(Individual individual, int p){
        this.individuals[p] = individual;
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
        System.out.println("Infeasibility cost2: " + infeasibilityCost);
        System.out.println("Biased fitness: " + biasedFitness);
        System.out.println("Diversity: " + diversity);
        System.out.println(" #Detailed cost# ");
        System.out.println("Travel cost: " + travelCost);
        System.out.println("Vehicle usage cost: " + vehicleUsageCost);
        System.out.println("Order allocation cost: " + orderDistribution.fitness);
        System.out.println("Time warp cost: " + timeWarpCost);
        System.out.println("Over load cost: " + overLoadCost);
        System.out.println("-------------------------------------");
    }




    public double getFitness(){
        return getFitness(true);
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

    public double getFitness(boolean includeOrderDistributionCost) {
        resetCosts();
        for (int p = 0; p < data.numberOfPeriods; p++){
            this.fitness += this.individuals[p].getPeriodicCost();
            this.travelCost += this.individuals[p].travelCost;

            this.infeasibilityCost += this.individuals[p].infeasibilityCost;
            this.vehicleUsageCost += this.individuals[p].vehicleUsageCost;
            this.timeWarpCost += this.individuals[p].timeWarpCost;
            this.overLoadCost += this.individuals[p].overLoadCost;

        }
        if (this.orderDistribution != null){
            this.orderDistributionCost = this.orderDistribution.getFitness();
        }
        return (includeOrderDistributionCost) ? this.fitness : this.fitness + this.orderDistributionCost ;
    }

    public ArrayList<Journey>[][] getJourneys(){
        for (int p  = 0; p < data.numberOfPeriods; p++ ){
            for (int vt = 0; vt < data.numberOfVehicleTypes; vt++){
                this.journeys[p][vt] = this.individuals[p].journeyList[p][vt];
            }
        }
        return this.journeys;
    }
}
