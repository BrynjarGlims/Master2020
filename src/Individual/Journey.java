package Individual;

import java.util.ArrayList;
import java.util.List;
import DataFiles.Data;
import DataFiles.DataReader;
import Genetic.OrderDistributionCrossover;
import Population.Population;
import ProductAllocation.OrderDistribution;
import Population.OrderDistributionPopulation;

public class Journey {


    Data data;
    int period;
    int vehicleType;
    int vehicleId;
    List<Trip> trips;

    public double drivingCost;
    public double timeWarpCost;
    public double overLoadCost;

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
    private double timeWarp;
    private double overLoad;

    public void updateFitness(OrderDistribution orderDistribution){
        currentTime = 0;
        timeWarp = 0;
        overLoad = 0;
        for (Trip trip : trips){
            updateTimes(trip);
        }

    }

    private void updateTimes(Trip trip){
        currentTime = Math.max(currentTime + data.distanceMatrix[data.numberOfCustomers][trip.customers.get(0)], data.customers[trip.customers.get(0)].timeWindow[period][0]);
        if (currentTime > data.customers[trip.customers.get(0)].timeWindow[period][1]){
            timeWarp += currentTime -  data.customers[trip.customers.get(0)].timeWindow[period][1];
            currentTime = data.customers[trip.customers.get(0)].timeWindow[period][1];
        }
        
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
        System.out.println(individual.journeyList[0][0]);
    }

    public String[] getArcsUsed(){

        return new String[1];
    }





}
