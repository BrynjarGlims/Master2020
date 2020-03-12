package Genetic;

import DataFiles.Customer;
import DataFiles.CustomerToTrip;
import DataFiles.Data;
import DataFiles.DataReader;
import Individual.*;
import Population.Population;
import ProductAllocation.OrderDistribution;
import Population.OrderDistributionPopulation;

import java.sql.SQLOutput;
import java.util.*;

public class RouteImprovements {


    public static Data data;
    public static OrderDistribution orderDistribution;

    public static void improveRoutes(Individual individual, OrderDistribution od){
        data = individual.data;
        orderDistribution = od;
        HashSet<Integer> customers = new HashSet<>();
        for (int i = 0 ; i < individual.data.numberOfCustomers ; i++){
            customers.add(i);
        }
        List<Integer> moves = new ArrayList<>();
        Collections.addAll(moves, 0);


        boolean improved;
        boolean temp;


//        individual.updateFitness();
//        System.out.println(individual.fitness);
//        insert1(individual, 5, 0);
//        individual.updateFitness();
//        System.out.println(individual.fitness);
        int[] taboo;
        int index = 0;
        int improvements;
        for (int c : customers){
            taboo = new int[3];
            improvements = 0;
            System.out.println("CUSTOMER: " + c);
            System.out.println("old fitness: " + individual.getFitness(true));
            for (int period = 0 ; period < data.numberOfPeriods ; period++){
                if (data.customers[c].requiredVisitPeriod[period] == 0){
                    continue;
                }
                improved = true;
                while (improved){
                    improved = false;
                    Collections.shuffle(moves);
                    for (int move : moves){
//                        if (move == taboo[0] || move == taboo[1] || move == taboo[2]){
//                            continue;
//                        }
                        switch (move){
                            case 0:
                                temp = insert1(individual, c, period);
                                improved =  improved || temp;
//                                System.out.println("move: " + move + " did improvement: " + temp);
                                if (temp){
                                    improvements++;
                                    taboo[index] = move;
                                    index = (index + 1) % 3;
                                }
                                break;
                            case 1:
                                temp = insert2(individual, c, period);
                                improved =  improved || temp;
                                if (temp){
                                    improvements++;
                                    taboo[index] = move;
                                    index = (index + 1) % 3;
                                }
                                break;
                            case 2:
                                temp = insert3(individual, c, period);
                                improved =  improved || temp;
                                if (temp){
                                    improvements++;
                                    taboo[index] = move;
                                    index = (index + 1) % 3;
                                }
                                break;
                            case 3:
                                temp = swap1(individual, c, period);
                                improved =  improved || temp;
                                if (temp){
                                    improvements++;
                                    taboo[index] = move;
                                    index = (index + 1) % 3;
                                }
                                break;
                            case 4:
                                temp = swap2(individual, c, period);
                                improved =  improved || temp;
                                if (temp){
                                    improvements++;
                                    taboo[index] = move;
                                    index = (index + 1) % 3;
                                }
                                break;
                            case 5:
                                temp = swap3(individual, c, period);
                                improved =  improved || temp;
                                if (temp){
                                    improvements++;
                                    taboo[index] = move;
                                    index = (index + 1) % 3;
                                }
                                break;
                            case 6:
                                temp = twoOpt1(individual, c, period);
                                improved =  improved || temp;
                                if (temp){
                                    improvements++;
                                    taboo[index] = move;
                                    index = (index + 1) % 3;
                                }
                                break;
                            case 7:
                                temp = twoOpt2(individual, c, period);
                                improved =  improved || temp;
                                if (temp){
                                    improvements++;
                                    taboo[index] = move;
                                    index = (index + 1) % 3;
                                }
                                break;
                            case 8:
                                temp = twoOpt3(individual, c, period);
                                improved =  improved || temp;
                                if (temp){
                                    improvements++;
                                    taboo[index] = move;
                                    index = (index + 1) % 3;
                                }
                                break;
                        }
                    }
                }
            }
            System.out.println("number of improvements: " + improvements);
            System.out.println("new fitness: " + individual.getFitness(true));
        }
    }



    private static boolean insert1(Individual individual, int customer, int period){
        //Tries to remove a single customer from its trip, and insert it after one of its nearest neighbors
        double improvementOfRemoval;
        double detoriorationOfInsertion;
        Trip trip1;
        Trip trip2;
        trip1 = individual.tripMap.get(period).get(customer);
        List<Integer> originalTrip1;
        List<Integer> originalTrip2;
        originalTrip1 = new LinkedList<>(trip1.customers);
        System.out.println("original 1 BEFORE FITNESS: " + originalTrip1);
        improvementOfRemoval = fitnessDifferenceOfRemoval(originalTrip1, trip1, customer);
        for (Customer neighbor : data.customers[customer].nearestNeighbors){
            if (neighbor.requiredVisitPeriod[period] == 0){
                continue;
            }
            trip2 = individual.tripMap.get(period).get(neighbor.customerID);
            originalTrip2 = new LinkedList<>(trip2.customers);

            System.out.println("original 2: " + originalTrip2);
            System.out.println("original 1: " + originalTrip1);
            if (trip1 == trip2){
                originalTrip2.remove(trip1.customerToTripIndexMap.get(customer));
            }
            System.out.println("customer: " + customer);
            System.out.println("neighbor: " + neighbor.customerID);
            detoriorationOfInsertion = fitnessDifferenceOfInsertion(originalTrip2, trip2, neighbor.customerID, customer);
            if (improvementOfRemoval > detoriorationOfInsertion){
                System.out.println("----------- BEFORE -------------");
                System.out.println("trip1: " + trip1.customers);
                System.out.println("trip2: " + trip2.customers);
                doRemovalInsertion(individual, trip1, trip2, customer, neighbor.customerID);
                System.out.println("----------- AFTER -------------");
                System.out.println("trip1: " + trip1.customers);
                System.out.println("trip2: " + trip2.customers);
                return true;
            }
        }
        return false;
    }

    private static boolean insert2(Individual individual, int customer, int period){
        return doubleInsertion(individual, customer, period, false);
    }

    private static boolean insert3(Individual individual, int customer, int period){
        return doubleInsertion(individual, customer, period, true);
    }

    private static boolean swap1(Individual individual, int customer, int period) {
        //swaps customer with neighbor if improvement
        Trip trip1;
        Trip trip2;
        trip1 = individual.tripMap.get(period).get(customer);
        List<Integer> originalTrip1;
        List<Integer> originalTrip2;
        originalTrip1 = new ArrayList<>(trip1.customers);
        for (Customer neighbor : data.customers[customer].nearestNeighbors) {
            if (neighbor.requiredVisitPeriod[period] == 0) {
                continue;
            }
            trip2 = individual.tripMap.get(period).get(neighbor.customerID);
            originalTrip2 = new LinkedList<>(trip2.customers);
            if (fitnessDifferenceOfSwap(originalTrip1, originalTrip2, trip1, trip2, customer, neighbor.customerID) > 0){
                performSwap(individual, trip1, trip2, customer, neighbor.customerID);
                return true;
            }
        }
        return false;
    }

    private static boolean swap2(Individual individual, int customer, int period){
        Trip trip1;
        Trip trip2;
        trip1 = individual.tripMap.get(period).get(customer);
        List<Integer> originalTrip1;
        List<Integer> originalTrip2;
        if (trip1.customers.size() - trip1.customerToTripIndexMap.get(customer) < 2){
            return false;
        }
        originalTrip1 = new LinkedList<>(trip1.customers);
        int succeedingCustomer = trip1.customers.get(trip1.customerToTripIndexMap.get(customer) + 1);

        double fitnessDifferenceOfSwap;
        for (Customer neighbor : data.customers[customer].nearestNeighbors) {
            if (neighbor.requiredVisitPeriod[period] == 0 || neighbor.customerID == succeedingCustomer) {
                continue;
            }
            trip2 = individual.tripMap.get(period).get(neighbor.customerID);
            originalTrip2 = new LinkedList<>(trip2.customers);
            fitnessDifferenceOfSwap = fitnessDifferenceOfSwapAndInsertion(originalTrip1, originalTrip2, trip1, trip2, customer, succeedingCustomer, neighbor.customerID);
            if (fitnessDifferenceOfSwap > 0){
                performSwap(individual, trip1, trip2, customer, neighbor.customerID);
                doRemovalInsertion(individual, trip1, trip2, succeedingCustomer, customer);
                return true;
            }
        }
        return false;
    }

    private static boolean swap3(Individual individual, int customer, int period){
        //swaps chosen customer and successor with neighbor and successor
        Trip trip1;
        Trip trip2;
        trip1 = individual.tripMap.get(period).get(customer);
        List<Integer> originalTrip1;
        List<Integer> originalTrip2;
        if (trip1.customers.size() - trip1.customerToTripIndexMap.get(customer) < 2){
            return false;
        }
        originalTrip1 = new LinkedList<>(trip1.customers);
        int succeedingCustomer1 = trip1.customers.get(trip1.customerToTripIndexMap.get(customer) + 1);


        int succeedingCustomer2;
        for (Customer neighbor : data.customers[customer].nearestNeighbors) {
            if (neighbor.requiredVisitPeriod[period] == 0){
                continue;
            }
            trip2 = individual.tripMap.get(period).get(neighbor.customerID);
            succeedingCustomer2 = trip2.customers.get(trip2.customerToTripIndexMap.get(neighbor.customerID) + 1);
            if (neighbor.customerID == succeedingCustomer1 || succeedingCustomer2 == customer || trip2.customers.size() - trip2.customerToTripIndexMap.get(neighbor.customerID) < 2) {
                continue;
            }
            originalTrip2 = new ArrayList<>(trip2.customers);

            double fitness = fitnessDifferenceOfDoubleSwap(originalTrip1, originalTrip2, trip1, trip2, customer, succeedingCustomer1, neighbor.customerID, succeedingCustomer2);
            if (fitness > 0){
                performSwap(individual, trip1, trip2, customer, neighbor.customerID);
                performSwap(individual, trip1, trip2, succeedingCustomer1, succeedingCustomer2);
                return true;
            }
        }
        return false;
    }

    private static boolean twoOpt1(Individual individual, int customer, int period){
        //if customer and neighbor is in the same route (trip), swap first successor with neighbor

        Trip trip1;
        Trip trip2;
        trip1 = individual.tripMap.get(period).get(customer);
        List<Integer> originalTrip1;
        List<Integer> originalTrip2;
        if (trip1.customers.size() - trip1.customerToTripIndexMap.get(customer) < 2 ||trip1.customers.size() < 3){
            return false;
        }
        originalTrip1 = new ArrayList<>(trip1.customers);

        int succeedingCustomer1 = trip1.customers.get(trip1.customerToTripIndexMap.get(customer) + 1);
        for (Customer neighbor : data.customers[customer].nearestNeighbors) {
            if (neighbor.requiredVisitPeriod[period] == 0){
                continue;
            }
            trip2 = individual.tripMap.get(period).get(neighbor.customerID);
            if (trip1 == trip2 && neighbor.customerID != succeedingCustomer1){
                originalTrip2 = new ArrayList<>(trip2.customers);
                if(fitnessDifferenceOfSwap(originalTrip1, originalTrip2, trip1, trip2, succeedingCustomer1, neighbor.customerID) > 0){
                    performSwap(individual, trip1, trip2, succeedingCustomer1, neighbor.customerID);
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean twoOpt2(Individual individual, int customer, int period){
        //if customer and neighbor does not come from same route (trip), replace successor with neighbor
        Trip trip1;
        Trip trip2;
        trip1 = individual.tripMap.get(period).get(customer);
        List<Integer> originalTrip1;
        List<Integer> originalTrip2;
        if (trip1.customers.size() - trip1.customerToTripIndexMap.get(customer) < 2 ||trip1.customers.size() < 3){
            return false;
        }
        originalTrip1 = new ArrayList<>(trip1.customers);

        int succeedingCustomer1 = trip1.customers.get(trip1.customerToTripIndexMap.get(customer) + 1);
        for (Customer neighbor : data.customers[customer].nearestNeighbors) {
            if (neighbor.requiredVisitPeriod[period] == 0){
                continue;
            }
            trip2 = individual.tripMap.get(period).get(neighbor.customerID);
            if (trip1 != trip2 && neighbor.customerID != succeedingCustomer1){
                originalTrip2 = new ArrayList<>(trip2.customers);
                if(fitnessDifferenceOfSwap(originalTrip1, originalTrip2, trip1, trip2, succeedingCustomer1, neighbor.customerID) > 0){
                    performSwap(individual, trip1, trip2, succeedingCustomer1, neighbor.customerID);
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean twoOpt3(Individual individual, int customer, int period){
        //if customer and neighbor not in same route (trip), swap customer - successor && neighbor - neighborSuccessor
        //with customer - neighborsuccessor && successor neighbor
        Trip trip1;
        Trip trip2;
        trip1 = individual.tripMap.get(period).get(customer);
        List<Integer> originalTrip1;
        List<Integer> originalTrip2;
        if (trip1.customers.size() - trip1.customerToTripIndexMap.get(customer) < 2 ||trip1.customers.size() < 3){
            return false;
        }
        originalTrip1 = new ArrayList<>(trip1.customers);
        int succeedingCustomer1 = trip1.customers.get(trip1.customerToTripIndexMap.get(customer) + 1);
        int succeedingCustomer2;
        for (Customer neighbor : data.customers[customer].nearestNeighbors) {
            if (neighbor.requiredVisitPeriod[period] == 0){
                continue;
            }
            trip2 = individual.tripMap.get(period).get(neighbor.customerID);
            if (trip1 != trip2 && neighbor.customerID != succeedingCustomer1){
                originalTrip2 = new ArrayList<>(trip2.customers);
                succeedingCustomer2 = trip2.customers.get(trip2.customerToTripIndexMap.get(neighbor.customerID) + 1);
                if(fitnessDifferenceOf3WaySwap(originalTrip1, originalTrip2, trip1, trip2, succeedingCustomer1, neighbor.customerID, succeedingCustomer2) > 0) {
                    perform3WaySwap(individual, trip1, trip2, succeedingCustomer1, neighbor.customerID, succeedingCustomer2);
                    return true;
                }
            }
        }
        return false;

    }

    private static boolean doubleInsertion(Individual individual, int customer, int period, boolean reverse){
        //inserts  customer and its successor after customers nearest neighbor if it gives improvement
        //If reverse is true, successor is placed prior to customer
        Trip trip1;
        Trip trip2;
        trip1 = individual.tripMap.get(period).get(customer);
        List<Integer> originalTrip1;
        List<Integer> originalTrip2;
        if (trip1.customers.size() - trip1.customerToTripIndexMap.get(customer) < 2){
            return false;
        }
        originalTrip1 = new LinkedList<>(trip1.customers);
        int succeedingCustomer1 = trip1.customers.get(trip1.customerToTripIndexMap.get(customer) + 1);
        double improvementOfRemoval;
        double detoriorationOfInsertion;
        int c1;
        int c2;
        improvementOfRemoval = fitnessDifferenceOfRemoval2Customers(originalTrip1, period, trip1.vehicleType, trip1.customerToTripIndexMap.get(succeedingCustomer1), trip1.customerToTripIndexMap.get(customer));
        for (Customer neighbor : data.customers[customer].nearestNeighbors){
            if (neighbor.requiredVisitPeriod[period] == 0 || neighbor.customerID == succeedingCustomer1){
                continue;
            }
            trip2 = individual.tripMap.get(period).get(neighbor.customerID);
            originalTrip2 = new LinkedList<>(trip2.customers);
            c1 = reverse ? succeedingCustomer1 : customer;
            c2 = reverse ? customer : succeedingCustomer1;
            detoriorationOfInsertion = fitnessDifferenceOfInsertion2Customers(originalTrip2, trip2, c1, c2, neighbor.customerID);
            if (improvementOfRemoval > detoriorationOfInsertion){
                doRemovalInsertion(individual, trip1, trip2, c1, neighbor.customerID);
                doRemovalInsertion(individual, trip1, trip2, c2, c1);
                return true;
            }
        }
        return false;
    }

//  ------------------------------HELPER FUNCTIONS-------------------------------------------------------

    private static double fitnessDifferenceOfRemoval(List<Integer> customerSequence, Trip trip, int customer){
        double initialFitness = FitnessCalculation.getTripFitness(customerSequence, trip.vehicleType, trip.period, orderDistribution.orderVolumeDistribution, data);
        customerSequence.remove(trip.customerToTripIndexMap.get(customer));
        System.out.println(customerSequence);
        double newFitness = FitnessCalculation.getTripFitness(customerSequence, trip.vehicleType, trip.period, orderDistribution.orderVolumeDistribution, data);
        customerSequence.add(trip.customerToTripIndexMap.get(customer), customer);
        return initialFitness - newFitness;
    }

    private static double fitnessDifferenceOfRemoval2Customers(List<Integer> customerSequence, int period, int vehicleType, int maxIndex, int minIndex){
        double initialFitness = FitnessCalculation.getTripFitness(customerSequence, vehicleType, period, orderDistribution.orderVolumeDistribution, data);
        customerSequence.remove(maxIndex);
        customerSequence.remove(minIndex);
        double newFitness = FitnessCalculation.getTripFitness(customerSequence, vehicleType, period, orderDistribution.orderVolumeDistribution, data);
        return initialFitness - newFitness;
    }

    private static double fitnessDifferenceOfInsertion(List<Integer> customerSequence, Trip trip, int targetCustomer, int insertedCustomer){
        //evaluates fitness difference of adding customer (insertedCustomer) after ctt.customer
        double initialFitness = FitnessCalculation.getTripFitness(customerSequence, trip.vehicleType, trip.period, orderDistribution.orderVolumeDistribution, data);
        int index = trip.customerToTripIndexMap.get(targetCustomer);
        System.out.println("this is index: " + index);
        System.out.println("customer sequence: " + customerSequence);
        customerSequence.add(index + 1, insertedCustomer);
        System.out.println("sequence: " + customerSequence);
        double newFitness = FitnessCalculation.getTripFitness(customerSequence, trip.vehicleType, trip.period, orderDistribution.orderVolumeDistribution, data);
        return newFitness - initialFitness;
    }

    private static double fitnessDifferenceOfInsertion2Customers(List<Integer> customerSequence, Trip trip, int insertedCustomer1, int insertedCustomer2, int targetCustomer){
        double initialFitness = FitnessCalculation.getTripFitness(customerSequence, trip.vehicleType, trip.period, orderDistribution.orderVolumeDistribution, data);
        customerSequence.add(trip.customerToTripIndexMap.get(targetCustomer) + 1, insertedCustomer2);
        customerSequence.add(trip.customerToTripIndexMap.get(targetCustomer) + 1, insertedCustomer1);
        double newFitness = FitnessCalculation.getTripFitness(customerSequence, trip.vehicleType, trip.period, orderDistribution.orderVolumeDistribution, data);
        return newFitness - initialFitness;
    }

    private static double fitnessDifferenceOfSwap(List<Integer> customerSequence1, List<Integer> customerSequence2, Trip trip1, Trip trip2, int customer1, int customer2){
        double oldFitness = combinedFitnessOf2Sequences(customerSequence1, customerSequence2, trip1.vehicleType, trip1.period, trip1.vehicleType, trip2.period);
        //perform swap and check new fitness
        customerSequence1.set(trip1.customerToTripIndexMap.get(customer1), customer2);
        customerSequence2.set(trip2.customerToTripIndexMap.get(customer2), customer1);
        double newFitness = combinedFitnessOf2Sequences(customerSequence1, customerSequence2, trip1.vehicleType, trip1.period, trip1.vehicleType, trip2.period);
        //swap back to original
        customerSequence1.set(trip1.customerToTripIndexMap.get(customer1), customer1);
        customerSequence2.set(trip2.customerToTripIndexMap.get(customer2), customer2);
        return newFitness - oldFitness;
    }

    private static double fitnessDifferenceOfDoubleSwap(List<Integer> customerSequence1, List<Integer> customerSequence2, Trip trip1, Trip trip2, int customer1, int succeedingCustomer1, int customer2, int succeedingCustomer2){
        double oldFitness = combinedFitnessOf2Sequences(customerSequence1, customerSequence2, trip1.vehicleType, trip1.period, trip2.vehicleType, trip2.period);
        customerSequence1.set(trip1.customerToTripIndexMap.get(customer1), customer2);
        customerSequence2.set(trip2.customerToTripIndexMap.get(customer2), customer1);
        customerSequence1.set(trip1.customerToTripIndexMap.get(succeedingCustomer1), succeedingCustomer2);
        customerSequence2.set(trip2.customerToTripIndexMap.get(succeedingCustomer2), succeedingCustomer1);


        double newFitness = combinedFitnessOf2Sequences(customerSequence1, customerSequence2, trip1.vehicleType, trip1.period, trip2.vehicleType, trip2.period);
        //swap back to original
        customerSequence1.set(trip1.customerToTripIndexMap.get(customer1), customer1);
        customerSequence2.set(trip2.customerToTripIndexMap.get(customer2), customer2);
        customerSequence1.set(trip1.customerToTripIndexMap.get(succeedingCustomer1), succeedingCustomer1);
        customerSequence2.set(trip2.customerToTripIndexMap.get(succeedingCustomer2), succeedingCustomer2);
        return newFitness - oldFitness;

    }

    private static double fitnessDifferenceOf3WaySwap(List<Integer> customerSequence1, List<Integer> customerSequence2, Trip trip1, Trip trip2, int succeedingCustomer1, int customer2, int succeedingCustomer2){
        //swaps ctt1 into ctt2, ctt2 into ctt3, and ctt3 into ctt1
        double oldFitness = combinedFitnessOf2Sequences(customerSequence1, customerSequence2, trip1.vehicleType, trip1.period, trip2.vehicleType, trip2.period);

        customerSequence1.set(trip1.customerToTripIndexMap.get(succeedingCustomer1), succeedingCustomer2);
        customerSequence2.set(trip2.customerToTripIndexMap.get(customer2), succeedingCustomer1);
        customerSequence2.set(trip2.customerToTripIndexMap.get(succeedingCustomer2), customer2);
        double newFitness = combinedFitnessOf2Sequences(customerSequence1, customerSequence2, trip1.vehicleType, trip1.period, trip2.vehicleType, trip2.period);
        //set back to original
        customerSequence1.set(trip1.customerToTripIndexMap.get(succeedingCustomer1), succeedingCustomer1);
        customerSequence2.set(trip2.customerToTripIndexMap.get(customer2), customer2);
        customerSequence2.set(trip2.customerToTripIndexMap.get(succeedingCustomer2), succeedingCustomer2);
        return newFitness - oldFitness;

    }

    private static double fitnessDifferenceOfSwapAndInsertion(List<Integer> customerSequence1, List<Integer> customerSequence2, Trip trip1, Trip trip2, int customer1, int succeedingCustomer1, int customer2){
        double oldFitness = combinedFitnessOf2Sequences(customerSequence1, customerSequence2, trip1.vehicleType, trip1.period, trip2.vehicleType, trip2.period);
        customerSequence1.set(trip1.customerToTripIndexMap.get(customer1), customer2);
        customerSequence2.set(trip2.customerToTripIndexMap.get(customer2), customer1);
        customerSequence1.remove(trip1.customerToTripIndexMap.get(succeedingCustomer1));
        customerSequence2.add(trip2.customerToTripIndexMap.get(customer2) + 1, succeedingCustomer1);
        double newFitness = combinedFitnessOf2Sequences(customerSequence1, customerSequence2, trip1.vehicleType, trip1.period, trip2.vehicleType, trip2.period);
        customerSequence2.remove(trip2.customerToTripIndexMap.get(customer2) + 1);
        customerSequence1.add(trip1.customerToTripIndexMap.get(succeedingCustomer1), succeedingCustomer1);
        customerSequence2.set(trip2.customerToTripIndexMap.get(customer2), customer2);
        customerSequence1.set(trip1.customerToTripIndexMap.get(customer1), customer1);
        return newFitness - oldFitness;

    }

    private static double combinedFitnessOf2Sequences(List<Integer> customerSequence1, List<Integer> customerSequence2, int vt1, int period1, int vt2, int period2){
        return FitnessCalculation.getTripFitness(customerSequence1, vt1, period1, orderDistribution.orderVolumeDistribution, data)
                + FitnessCalculation.getTripFitness(customerSequence2, vt2, period2, orderDistribution.orderVolumeDistribution, data);
    }

    private static void doRemovalInsertion(Individual individual, Trip trip1, Trip trip2, int removeCustomer, int targetCustomer){
        System.out.println("old trip1: " + trip1.customers);
        System.out.println("old trip2: " + trip2.customers);
        trip1.removeCustomer(removeCustomer);
        trip2.addCustomer(removeCustomer, trip2.customerToTripIndexMap.get(targetCustomer) + 1);
        System.out.println("new trip1: " + trip1.customers);
        System.out.println("new trip2: " + trip2.customers);

        individual.tripMap.get(trip2.period).put(removeCustomer, trip2);
    }


    private static void perform3WaySwap(Individual individual, Trip trip1, Trip trip2, int succeedingCustomer1, int customer2, int succeedingCustomer2){
        int setIndex1 = trip1.customerToTripIndexMap.get(succeedingCustomer1);
        int setIndex2 = trip2.customerToTripIndexMap.get(customer2);
        trip1.setCustomer(succeedingCustomer2, setIndex1);
        trip2.setCustomer(succeedingCustomer1, setIndex2);
        trip2.setCustomer(customer2, setIndex2 + 1);
        individual.tripMap.get(trip2.period).put(succeedingCustomer1, trip2);
        individual.tripMap.get(trip1.period).put(succeedingCustomer2, trip1);
    }

    private static void performSwap(Individual individual, Trip trip1, Trip trip2, int customer1, int customer2){
        int setIndex1 = trip1.customerToTripIndexMap.get(customer1);
        int setIndex2 = trip2.customerToTripIndexMap.get(customer2);
        trip1.setCustomer(customer2, setIndex1);
        trip2.setCustomer(customer1, setIndex2);
        individual.tripMap.get(trip1.period).put(customer1, trip2);
        individual.tripMap.get(trip2.period).put(customer2, trip1);
    }



    public static void main(String[] args){
        Data data = DataReader.loadData();
        Population population = new Population(data);
        OrderDistributionPopulation odp = new OrderDistributionPopulation(data);
        GiantTourCrossover GTC = new GiantTourCrossover(data);
        OrderDistributionCrossover ODC = new OrderDistributionCrossover(data);
        odp.initializeOrderDistributionPopulation(population);
        OrderDistribution firstOD = odp.getRandomOrderDistribution();
        population.setOrderDistributionPopulation(odp);
        population.initializePopulation(firstOD);
        Individual individual = population.getRandomIndividual();

        RouteImprovements.improveRoutes(individual, individual.orderDistribution);

//        int move = 5;
//        for (int i = 0 ; i < 10 ; i++)
//        switch (move){
//            case 1:
//                System.out.println(move);
//            case 5:
//                System.out.println(move);
//        }
//
//        System.out.println("finished");
//
//        HashSet<Integer> moves = new HashSet<>();
//        for (int i = 0 ;  i < 9 ; i++){
//            moves.add(i);
//        }
//        for (int move : moves){
//            System.out.println(move);
//        }

//        List<Integer> a = new LinkedList<>();
//        a.add(5);
//        a.add(4);
//        System.out.println(a);
//        a.add(a.size(),1);
//        a.set(1, 100);
//
//        List<Integer> b = new ArrayList<>(a);
//
//        System.out.println("b: " + b);
//        b.add(4);
//        b.add(123);
//        System.out.println("b: " + b);
//        System.out.println(a);

//        System.out.println(individual.giantTour.chromosome[0][5]);
//        System.out.println(individual.giantTourSplit.chromosome[0][5]);
//        System.out.println(individual.customerToTrips[0].length);
//        System.out.println(Arrays.toString(individual.customerToTrips[0]));


    }



}
