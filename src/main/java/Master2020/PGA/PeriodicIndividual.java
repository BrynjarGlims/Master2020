package Master2020.PGA;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.Parameters;
import Master2020.Genetic.FitnessCalculation;
import Master2020.Genetic.PenaltyControl;
import Master2020.Individual.Individual;
import Master2020.Individual.Journey;
import Master2020.Individual.Trip;
import Master2020.ProductAllocation.OrderDistribution;

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

    public PenaltyControl penaltyControl;


    public PeriodicIndividual(Data data, PenaltyControl penaltyControl){
        this.data = data;
        this.penaltyControl = penaltyControl;
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
        System.out.println("Order distribution scaling factor " + orderDistribution.orderScalingFactor);
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
        updateFitness();
        return this.timeWarpCost == 0 && this.overLoadCost/Parameters.initialOverLoadPenalty <= Parameters.indifferenceValue;
    }

    public double getFitness() {
        updateFitness();
        return fitness;
    }

    public void updateFitness(){
        double[] fitnesses =  FitnessCalculation.getIndividualFitness(data, journeys, orderDistribution, 1, penaltyControl.timeWarpPenalty, penaltyControl.overLoadPenalty );
        this.travelCost = fitnesses[0];
        this.timeWarpCost = fitnesses[1];
        this.overLoadCost = fitnesses[2];
        this.vehicleUsageCost = fitnesses[3];
        this.infeasibilityCost = fitnesses[1] + fitnesses[2];
        this.orderDistributionCost = orderDistribution.getFitness();
        this.fitness = this.travelCost + this.timeWarpCost + this.overLoadCost + this.vehicleUsageCost + this.orderDistributionCost;
    }

    public void initializeJourneys(int p){
        for (int vt = 0; vt < data.numberOfVehicleTypes; vt++){
            this.journeys[p][vt] = this.individuals[p].journeyList[0][vt];
        }
    }



    public PGASolution createPGASolution(){
        Individual newIndividual = new Individual(this.data, null, false, -1, penaltyControl);
        updateJourneysToPeriodicConfiguration();
        newIndividual.journeyList = this.journeys;
        newIndividual.orderDistribution = orderDistribution;
        newIndividual.setGiantTourFromJourneys();
        newIndividual.setTripListFromJourneys();
        newIndividual.setTripMapFromTripList();
        PGASolution pgaSolution = new PGASolution(orderDistribution, journeys);
        pgaSolution.setIndividual(newIndividual);
        return pgaSolution;
    }

    private void updateJourneysToPeriodicConfiguration(){
        for (int p = 0; p < data.numberOfPeriods; p++){
            for (int vt = 0; vt < data.numberOfVehicleTypes; vt ++){
                for (Journey journey : this.journeys[p][vt]){
                    for (Trip trip : journey.trips){
                        trip.period = p;
                    }
                }
            }
        }

    }

    public int compareTo(PeriodicIndividual periodicIndividual) {
        return this.getFitness() < periodicIndividual.getFitness() ? 1 : -1; // TODO: 04.03.2020 Sort by biased fitness and not fitness
    }


}
