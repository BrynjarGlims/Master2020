package Master2020.Individual;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.DataFiles.Parameters;
import Master2020.Genetic.OrderDistributionCrossover;
import Master2020.Genetic.PenaltyControl;
import Master2020.Population.Population;
import Master2020.ProductAllocation.OrderDistribution;
import Master2020.Population.OrderDistributionPopulation;

public class Journey {


    public Data data;
    public int period;
    public int vehicleType;
    public int vehicleId;
    public List<Trip> trips;
    public double vehicleCost;


    public double journeyCost;

    private double currentTime;
    private double travelDistance;
    private double timeWarp;
    private double overLoad;
    private double travelCost;

    public Origin ID;

    public Journey(Data data, int period, int vehicleType, int vehicleId){
        this.data = data;
        this.period = period;
        this.vehicleType = vehicleType;
        this.vehicleId = vehicleId;
        vehicleCost = data.vehicleTypes[vehicleType].usageCost;
        trips = new ArrayList<>();
    }

    public void addTrip(Trip trip){
        trips.add(trip);
        trip.journey = this;

    }

    public double getFitnessWithVehicleCost(OrderDistribution orderDistribution){
        updateFitness(orderDistribution);
        return travelCost + data.vehicleTypes[vehicleType].usageCost;
    }


    public double[] updateFitness(OrderDistribution orderDistribution){
        return updateFitness(orderDistribution, 1, Parameters.initialTimeWarpPenalty, Parameters.initialOverLoadPenalty);
    }

    public double[] updateFitness(OrderDistribution orderDistribution, double penaltyMultiplier, double timeWarpPenalty, double overLoadPenalty){ //include cost of using vehicle?
        currentTime = 0;
        timeWarp = 0;
        overLoad = 0;
        travelDistance = 0;
        boolean firstTrip = true;
        for (Trip trip : trips){
            if (trip.customers.size() == 0){
                continue;
            }
            if (!firstTrip){
                currentTime += data.vehicleTypes[vehicleType].loadingTimeAtDepot;
            }
            else{
                firstTrip = false;
            }
            updateTimes(trip);
            updateOverload(trip, orderDistribution);
        }
        travelCost = travelDistance*data.vehicleTypes[vehicleType].travelCost;
        double timeWarpCost = timeWarp * timeWarpPenalty * penaltyMultiplier;
        double overLoadCost = overLoad * overLoadPenalty * penaltyMultiplier;
        return new double[]{travelCost, timeWarpCost, overLoadCost, vehicleCost};
    }

    public double getTotalFitness(OrderDistribution orderDistribution){
        return getTotalFitness(orderDistribution, 1, Parameters.initialTimeWarpPenalty, Parameters.initialTimeWarpPenalty);
    }



    public double getTotalFitness(OrderDistribution orderDistribution, double penaltyMultiplier, double timeWarpPenalty, double overLoadPenalty){
        double[] fitnesses = updateFitness(orderDistribution, penaltyMultiplier, timeWarpPenalty, overLoadPenalty);
        return fitnesses[0] + fitnesses[1] + fitnesses[2] + fitnesses[3]; //travel + timewarp + overload + vehicle use
    }

    private void updateTimes(Trip trip){
        if (trip.customers.isEmpty()){
            return;
        }
        if (trip.customers.size() == 0){
            System.out.println("This should not bve prunted");
            return;
        }

        //initialize
        int previousCustomer = data.numberOfCustomers;
        for ( int customerID : trip.customers){
            currentTime += data.distanceMatrix[previousCustomer][customerID];
            currentTime = Math.max(currentTime, data.customers[customerID].timeWindow[period][0]);
            if (currentTime > data.customers[customerID].timeWindow[period][1]){
                timeWarp +=  currentTime - data.customers[customerID].timeWindow[period][1];
                currentTime = data.customers[customerID].timeWindow[period][1];
            }
            currentTime +=  data.customers[customerID].totalUnloadingTime;
            travelDistance += data.distanceMatrix[previousCustomer][customerID];
            previousCustomer = customerID;
        }
        currentTime += data.distanceMatrix[previousCustomer][data.numberOfCustomers];
        travelDistance += data.distanceMatrix[previousCustomer][data.numberOfCustomers];
        if (currentTime > Parameters.maxJourneyDuration){
            timeWarp += currentTime - Parameters.maxJourneyDuration;
            currentTime = Parameters.maxJourneyDuration;
        }
    }

    public boolean compareToOldJourney(Master2020.PR.Journey journey){
        if (this.trips.size() != journey.paths.length){
            return false;
        }
        for (int i = 0; i < this.trips.size(); i++){
            if(!this.trips.get(i).isEqualToPath(journey.paths[i])){
                return false;
            }
            else{
                System.out.println("Journey found");
            }
        }
        return true;
    }

    private void updateOverload(Trip trip, OrderDistribution orderDistribution){
        double load = 0;
        for (int customer : trip.customers){
            load += orderDistribution.getOrderVolumeDistribution(period,customer);
        }
        overLoad += Math.max(0, load - data.vehicleTypes[vehicleType].capacity);
    }

    public String toString(){
        String out = "";
        for (Trip trip : trips){
            out += "trip: " + trip.tripIndex;
            out += trip.toString();
        }
        return out;
    }


    public static void main(String[] args){
        Data data = DataReader.loadData();
        PenaltyControl penaltyControl = new PenaltyControl(Parameters.initialTimeWarpPenalty, Parameters.initialOverLoadPenalty);
        Population population = new Population(data);
        OrderDistributionPopulation odp = new OrderDistributionPopulation(data);
        OrderDistributionCrossover ODC = new OrderDistributionCrossover(data);
        odp.initializeOrderDistributionPopulation(population);
        OrderDistribution firstOD = odp.getRandomOrderDistribution();
        population.setOrderDistributionPopulation(odp);
        population.initializePopulation(firstOD, penaltyControl);
        Individual individual = population.getRandomIndividual();
        AdSplit.adSplitPlural(individual, Parameters.initialTimeWarpPenalty, Parameters.initialOverLoadPenalty);
        Individual parent1 = population.getRandomIndividual();
        Individual parent2 = population.getRandomIndividual();
        System.out.println(parent1.getFitness(true));
        Individual child = Master2020.Genetic.GiantTourCrossover.crossOver(parent1, parent2, parent1.orderDistribution, penaltyControl);
        System.out.println(child.getFitness(true));
        Master2020.Genetic.Education.improveRoutes(child, child.orderDistribution, Parameters.initialTimeWarpPenalty, Parameters.initialOverLoadPenalty);
        System.out.println(child.getFitness(true));
    }


    public HashSet<String> getArcsUsed(){
        HashSet<String> arcs = new HashSet<>();
        String previousCustomer;
        
        for (Trip trip : trips){
            previousCustomer = "#";
            for (int customerID: trip.customers){
                arcs.add(period + vehicleType + previousCustomer + customerID);  // # indicates depot. First entry is from, second entry is to
                previousCustomer = Integer.toString(customerID);
            }
            arcs.add(period + vehicleType + previousCustomer + "#");
        }
        return arcs;  //// TODO: 23/03/2020 Make object store the arcs it uses for every adsplit
    }
}
