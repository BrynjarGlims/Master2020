package Individual;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import DataFiles.Data;
import DataFiles.DataReader;
import DataFiles.Parameters;
import Genetic.Education;
import Genetic.OrderDistributionCrossover;
import Population.Population;
import ProductAllocation.OrderDistribution;
import Population.OrderDistributionPopulation;
import scala.xml.PrettyPrinter;

public class Journey {


    public Data data;
    public int period;
    public int vehicleType;
    public int vehicleId;
    public List<Trip> trips;

    public double journeyCost;

    public Journey(Data data, int period, int vehicleType, int vehicleId){
        this.data = data;
        this.period = period;
        this.vehicleType = vehicleType;
        this.vehicleId = vehicleId;
        trips = new ArrayList<>();
    }

    public void addTrip(Trip trip){
        trips.add(trip);
        trip.journey = this;

    }

    private double currentTime;
    private double travelDistance;
    private double timeWarp;
    private double overLoad;

    public double updateFitness(OrderDistribution orderDistribution){ //include cost of using vehicle?
        currentTime = 0;
        timeWarp = 0;
        overLoad = 0;
        travelDistance = 0;
        boolean firstTrip = true;
        for (Trip trip : trips){
            if (!firstTrip){
                currentTime += data.vehicleTypes[vehicleType].loadingTimeAtDepot;
            }
            updateTimes(trip);
            updateOverload(trip, orderDistribution);
            firstTrip = false;
        }
        journeyCost = timeWarp* Parameters.initialTimeWarpPenalty + overLoad*Parameters.penaltyFactorForOverFilling + travelDistance*data.vehicleTypes[vehicleType].travelCost;
        return journeyCost;
    }

    private void updateTimes(Trip trip){
        if (trip.customers.isEmpty()){
            return;
        }
        currentTime = Math.max(currentTime + data.distanceMatrix[data.numberOfCustomers][trip.customers.get(0)], data.customers[trip.customers.get(0)].timeWindow[period][0]);
        travelDistance += data.distanceMatrix[data.numberOfCustomers][trip.customers.get(0)];
        if (currentTime > data.customers[trip.customers.get(0)].timeWindow[period][1]){
            timeWarp += currentTime -  data.customers[trip.customers.get(0)].timeWindow[period][1];
            currentTime = data.customers[trip.customers.get(0)].timeWindow[period][1];
        }
        currentTime += data.customers[trip.customers.get(0)].totalUnloadingTime;
        for (int i = 0 ; i < trip.customers.size() - 1 ; i++){
            currentTime = Math.max(currentTime + data.distanceMatrix[trip.customers.get(i)][trip.customers.get(i + 1)], data.customers[trip.customers.get(i + 1)].timeWindow[period][0]);
            travelDistance += data.distanceMatrix[trip.customers.get(i)][trip.customers.get(i + 1)];
            if (currentTime > data.customers[trip.customers.get(i + 1)].timeWindow[period][1]){
                timeWarp += currentTime -  data.customers[trip.customers.get(i + 1)].timeWindow[period][1];
                currentTime = data.customers[trip.customers.get(i + 1)].timeWindow[period][1];
            }
            currentTime += data.customers[trip.customers.get(i + 1)].totalUnloadingTime;
        }
        currentTime += data.distanceMatrix[trip.customers.get(trip.customers.size() - 1)][data.numberOfCustomers];
        travelDistance += data.distanceMatrix[trip.customers.get(trip.customers.size() - 1)][data.numberOfCustomers];
        if (currentTime > Parameters.maxJourneyDuration){
            timeWarp += currentTime - Parameters.maxJourneyDuration;
            currentTime = Parameters.maxJourneyDuration;
        }
    }

    private void updateOverload(Trip trip, OrderDistribution orderDistribution){
        double load = 0;
        for (int customer : trip.customers){
            load += orderDistribution.orderVolumeDistribution[period][customer];
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
        Population population = new Population(data);
        OrderDistributionPopulation odp = new OrderDistributionPopulation(data);
        OrderDistributionCrossover ODC = new OrderDistributionCrossover(data);
        odp.initializeOrderDistributionPopulation(population);
        OrderDistribution firstOD = odp.getRandomOrderDistribution();
        population.setOrderDistributionPopulation(odp);
        population.initializePopulation(firstOD);
        Individual individual = population.getRandomIndividual();
        AdSplit.adSplitPlural(individual);
        Individual parent1 = population.getRandomIndividual();
        Individual parent2 = population.getRandomIndividual();
        Individual child = Genetic.GiantTourCrossover.crossOver(parent1, parent2, parent1.orderDistribution);
        Genetic.Education.improveRoutes(child, child.orderDistribution);
//


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