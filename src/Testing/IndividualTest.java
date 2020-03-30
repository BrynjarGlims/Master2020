package Testing;

import DataFiles.Customer;
import DataFiles.Data;
import DataFiles.DataReader;
import DataFiles.Parameters;
import Individual.Individual;
import Individual.Journey;
import Individual.Trip;
import MIP.OrderAllocationModel;
import Population.Population;
import ProductAllocation.OrderDistribution;
import Population.OrderDistributionPopulation;
import Individual.AdSplit;
import StoringResults.Result;
import gurobi.GRBException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class IndividualTest {
    public static void checkIfIndividualIsComplete( Individual individual) throws Exception {
        for (int p = 0; p < individual.data.numberOfPeriods; p++){
            for (int i = 0; i < individual.data.numberOfCustomers; i++){
                if (individual.tripMap.get(p).containsKey(i)){
                    boolean customerFound = false;
                    for (int vt = 0; vt < individual.data.numberOfVehicleTypes; vt++){
                        if (individual.giantTour.chromosome[p][vt].contains(i)){
                            customerFound = true;
                        }
                    }
                    if (!customerFound){
                        System.out.println("Cannot find trip for period "+p+ " customer " +i );
                        throw new Exception();
                    }
                }
            }
        }
    }


    public static boolean testTripMap(Individual individual){
        Data data = individual.data;
        for (int p = 0 ; p < data.numberOfPeriods ; p++){
            for (Customer customer : data.customers){
                if (customer.requiredVisitPeriod[p] == 1){
                    if (!individual.tripMap.get(p).containsKey(customer.customerID)){
                        System.out.println("missing customer: " + customer.customerID + " in period " + p);
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static double getTrueIndividualFitness(Individual individual){
        Data data = individual.data;
        double objective = 0;
        double vehicleUseCost = 0;
        double travelCost = 0;
        double overtimeDepotCost = 0;
        for (int p = 0; p < data.numberOfPeriods; p++){
            for (int vt = 0; vt < data.numberOfVehicleTypes; vt++){
                if (individual.journeyList[p][vt] == null){
                    continue;
                }
                for (Journey journey : individual.journeyList[p][vt]){
                    vehicleUseCost += data.vehicleTypes[journey.vehicleType].usageCost;
                    for (Trip trip : journey.trips){
                        int previousCustomer = data.numberOfCustomers;
                        for (int customerID : trip.customers){
                            travelCost += data.vehicleTypes[vt].travelCost*data.distanceMatrix[previousCustomer][customerID];
                            previousCustomer = customerID;
                        }
                        travelCost += data.vehicleTypes[vt].travelCost*data.distanceMatrix[previousCustomer][data.numberOfCustomers];
                    }

                }
            }
            overtimeDepotCost += Parameters.overtimeCost[p]* Math.max(0, individual.orderDistribution.volumePerPeriod[p]-Parameters.overtimeLimit[p]);
        }
        objective = vehicleUseCost + travelCost + overtimeDepotCost;
        System.out.println("Travelcost: " +travelCost);
        System.out.println("Vehicle cost: " + vehicleUseCost);
        System.out.println("Overtime Depot: " + overtimeDepotCost);

        return objective;

    }

    public static void isMissingCustomersAdded( HashMap<Integer, HashSet<Integer>> missingCustomers, Individual individual) {
        for ( int p : missingCustomers.keySet()){
            for (int i : missingCustomers.get(p)){
                boolean foundCustomer = false;
                for (int vt = 0; vt < individual.data.numberOfVehicleTypes; vt ++){
                    if (individual.giantTour.chromosome[p][vt].contains(i)){
                        foundCustomer = true;
                        break;
                    }
                }
                if (!foundCustomer){
                    System.out.println("Cannot find trip for period "+p+ " customer " +i );
                    try {
                        throw new Exception();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        }
    }

    public static void checkDiversityIndividual(Individual individual){
        if (individual.getDiversity() == 0){
            System.out.println("------------------------------------------------------------");
            System.out.println("Diversity not calculated properly");
        }
    }

    /*public static void whyInfeasbile(Individual individual){
        for (Journey journey : individual.journeyList){
            for (Trip )

        }

    }

     */

    public static void main(String[] args) throws IOException, GRBException {
        Data data = DataReader.loadData();
        Individual individual = new Individual(data);
        ArrayList<Integer>[][] chromosome = new ArrayList[data.numberOfPeriods][data.numberOfVehicleTypes];
        for (int p = 0; p < data.numberOfPeriods; p++){
            for (int vt = 0; vt < data.numberOfVehicleTypes; vt++){
                if (vt == 2){
                    if (p == 0){
                        chromosome[p][vt] = new ArrayList<>(Arrays.asList(1,2,6,5,3,4));
                        //chromosome[p][vt] = new ArrayList<>();
                    }
                    if (p == 1){
                        chromosome[p][vt] = new ArrayList<>(Arrays.asList(1,0,3,6,2,5,4));
                    }
                    if (p == 2){
                        chromosome[p][vt] = new ArrayList<>(Arrays.asList(3,5,2));
                        //chromosome[p][vt] = new ArrayList<>();
                    }
                    if (p == 3){
                        chromosome[p][vt] = new ArrayList<>(Arrays.asList(1,3,0,2,5,4,6));
                        //chromosome[p][vt] = new ArrayList<>();
                    }
                    if (p == 4){
                        chromosome[p][vt] = new ArrayList<>(Arrays.asList(1,5,6,2,0,3));
                        //chromosome[p][vt] = new ArrayList<>();
                    }
                    if (p == 5){
                        chromosome[p][vt] = new ArrayList<>(Arrays.asList(3,2));
                        //chromosome[p][vt] = new ArrayList<>();
                    }
                }
                else{
                    chromosome[p][vt] = new ArrayList<Integer>();
                }
            }
        }

        individual.giantTour.setChromosome(chromosome);
        OrderDistribution od = new OrderDistribution(data);
        Population population = new Population(data);
        OrderDistributionPopulation odp = new OrderDistributionPopulation(data);
        odp.initializeOrderDistributionPopulation(population);
        OrderDistribution firstOD = odp.getRandomOrderDistribution();
        individual.orderDistribution = firstOD;
        AdSplit.adSplitPlural(individual);
        OrderAllocationModel orderAllocationModel = new OrderAllocationModel(data);

        OrderDistribution optimalOD = orderAllocationModel.createOptimalOrderDistribution(individual);
        AdSplit.adSplitPlural(individual);
        System.out.println(individual.getFitness(true));
        individual.printDetailedFitness();
        System.out.println("Done");
        Result res = new Result(individual);
        res.store();


    }

}
