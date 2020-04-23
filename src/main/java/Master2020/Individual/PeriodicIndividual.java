package Master2020.Individual;

import Master2020.DataFiles.Data;
import Master2020.Population.PeriodicPopulation;
import Master2020.ProductAllocation.OrderDistribution;
import gurobi.GRB;

public class PeriodicIndividual {

    public Data data;
    public Individual[] individuals;
    public double fitness = Double.MAX_VALUE;
    public OrderDistribution orderDistribution;

    public double infeasibilityCost;
    public double travelCost;
    public double vehicleUsageCost;
    public double timeWarpCost;
    public double overLoadCost;
    public double orderDistributionCost;


    public PeriodicIndividual(Data data){
        this.data = data;
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

    public double getFitness() {
        this.fitness = 0;
        for (int p = 0; p < data.numberOfPeriods; p++){
            this.fitness += this.individuals[p].getPeriodicCost();
            this.travelCost += this.individuals[p].travelCost;

            this.infeasibilityCost += this.individuals[p].infeasibilityCost;
            this.vehicleUsageCost += this.individuals[p].vehicleUsageCost;
            this.timeWarpCost += this.individuals[p].timeWarpCost;
            this.overLoadCost += this.individuals[p].overLoadCost;

        }
        public double orderDistributionCost;
        return this.fitness;
    }
}
