package Genetic;

import DataFiles.*;
import Individual.*;
import Population.Population;
import ProductAllocation.OrderDistribution;
import Population.OrderDistributionPopulation;

import java.util.*;

public class Education {


    public static Data data;
    public static OrderDistribution orderDistribution;

    public static void improveRoutes(Individual individual, OrderDistribution od) {
        data = individual.data;
        orderDistribution = od;
        HashSet<Integer> customers = new HashSet<>();
        for (int i = 0; i < individual.data.numberOfCustomers; i++) {
            customers.add(i);
        }
        List<Integer> moves = new ArrayList<>();
        Collections.addAll(moves, 0, 1, 2, 3, 4, 5, 6, 7, 8);
        boolean improved;
        boolean temp;
        boolean tabooed;
        int[] taboo;
        int index = 0;
        individual.updateFitness();
        int totalImprovements = 0;
        for (int c : customers) {
            taboo = new int[Parameters.EducationTabooSize];
            for (int period = 0; period < data.numberOfPeriods; period++) {
                if (data.customers[c].requiredVisitPeriod[period] == 0) {
                    continue;
                }
                improved = true;
                while (improved) {
                    Collections.shuffle(data.customers[c].nearestNeighbors);
                    improved = false;
                    Collections.shuffle(moves);
                    for (int move : moves) {
                        tabooed = false;
                        for (int t : taboo) {
                            if (move == t) {
                                tabooed = true;
                            }
                        }
                        if (tabooed) {
                            continue;
                        }
                        switch (move) {
                            case 0:
                                temp = insert1(individual, c, period);
                                improved = improved || temp;
                                if (temp) {
                                    totalImprovements++;
                                    taboo[index] = move;
                                    index = (index + 1) % Parameters.EducationTabooSize;
                                }
                                break;
                            case 1:
                                temp = insert2(individual, c, period);
                                improved = improved || temp;
                                if (temp) {
                                    totalImprovements++;
                                    taboo[index] = move;
                                    index = (index + 1) % Parameters.EducationTabooSize;
                                }
                                break;
                            case 2:
                                temp = insert3(individual, c, period);
                                improved = improved || temp;
                                if (temp) {
                                    totalImprovements++;
                                    taboo[index] = move;
                                    index = (index + 1) % Parameters.EducationTabooSize;
                                }
                                break;
                            case 3:
                                temp = swap1(individual, c, period);
                                improved = improved || temp;
                                if (temp) {
                                    totalImprovements++;
                                    taboo[index] = move;
                                    index = (index + 1) % Parameters.EducationTabooSize;
                                }
                                break;
                            case 4:
                                temp = swap2(individual, c, period);
                                improved = improved || temp;
                                if (temp) {
                                    totalImprovements++;
                                    taboo[index] = move;
                                    index = (index + 1) % Parameters.EducationTabooSize;
                                }
                                break;
                            case 5:
                                temp = swap3(individual, c, period);
                                improved = improved || temp;
                                if (temp) {
                                    totalImprovements++;
                                    taboo[index] = move;
                                    index = (index + 1) % Parameters.EducationTabooSize;
                                }
                                break;
                            case 6:
                                temp = twoOpt1(individual, c, period);
                                improved = improved || temp;
                                if (temp) {
                                    totalImprovements++;
                                    taboo[index] = move;
                                    index = (index + 1) % Parameters.EducationTabooSize;
                                }
                                break;
                            case 7:
                                temp = twoOpt2(individual, c, period);
                                improved = improved || temp;
                                if (temp) {
                                    totalImprovements++;
                                    taboo[index] = move;
                                    index = (index + 1) % Parameters.EducationTabooSize;
                                }
                                break;
                            case 8:
                                temp = twoOpt3(individual, c, period);
                                improved = improved || temp;
                                if (temp) {
                                    totalImprovements++;
                                    taboo[index] = move;
                                    index = (index + 1) % Parameters.EducationTabooSize;
                                }
                                break;
                        }
                    }
                }
            }
        }
        if (Parameters.verbose){
            System.out.println("total totalImprovements in Education: " + totalImprovements);
        }
        individual.setGiantTourFromTrips();
        AdSplit.adSplitPlural(individual);
        individual.updateFitness();
    }


    private static boolean insert1(Individual individual, int customer, int period) {
        //Tries to remove a single customer from its trip, and insert it after one of its nearest neighbors
        Trip trip1;
        Trip trip2;
        trip1 = individual.tripMap.get(period).get(customer);
        List<Integer> originalTrip1;
        List<Integer> originalTrip2;
        originalTrip1 = new LinkedList<>(trip1.customers);
        for (Customer neighbor : data.customers[customer].nearestNeighbors) {
            if (neighbor.requiredVisitPeriod[period] == 0) {
                continue;
            }
            trip2 = individual.tripMap.get(period).get(neighbor.customerID);
            originalTrip2 = new LinkedList<>(trip2.customers);
            if (fitnessDifferenceOfRemovalInsertion(originalTrip1, originalTrip2, trip1, trip2, customer, neighbor.customerID) > 0) {
                doRemovalInsertion(individual, trip1, trip2, customer, neighbor.customerID);
                return true;
            }
        }
        return false;
    }

    private static boolean insert2(Individual individual, int customer, int period) {
        return doubleInsertion(individual, customer, period, false);
    }

    private static boolean insert3(Individual individual, int customer, int period) {
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
            if (fitnessDifferenceOfSwap(originalTrip1, originalTrip2, trip1, trip2, customer, neighbor.customerID) > 0) {
                performSwap(individual, trip1, trip2, customer, neighbor.customerID);
                return true;
            }
        }
        return false;
    }

    private static boolean swap2(Individual individual, int customer, int period) {
        Trip trip1;
        Trip trip2;
        trip1 = individual.tripMap.get(period).get(customer);
        List<Integer> originalTrip1;
        List<Integer> originalTrip2;
        if (trip1.customers.size() - trip1.customerToTripIndexMap.get(customer) < 2) {
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
            if (fitnessDifferenceOfSwap > 0) {
                performSwap(individual, trip1, trip2, customer, neighbor.customerID);
                doRemovalInsertion(individual, trip1, trip2, succeedingCustomer, customer);
                return true;
            }
        }
        return false;
    }

    private static boolean swap3(Individual individual, int customer, int period) {
        //swaps chosen customer and successor with neighbor and successor
        Trip trip1;
        Trip trip2;
        trip1 = individual.tripMap.get(period).get(customer);
        List<Integer> originalTrip1;
        List<Integer> originalTrip2;
        if (trip1.customers.size() - trip1.customerToTripIndexMap.get(customer) < 2) {
            return false;
        }
        originalTrip1 = new LinkedList<>(trip1.customers);
        int succeedingCustomer1 = trip1.customers.get(trip1.customerToTripIndexMap.get(customer) + 1);


        int succeedingCustomer2;
        for (Customer neighbor : data.customers[customer].nearestNeighbors) {
            if (neighbor.requiredVisitPeriod[period] == 0) {
                continue;
            }
            trip2 = individual.tripMap.get(period).get(neighbor.customerID);
            if (trip2.customers.size() - trip2.customerToTripIndexMap.get(neighbor.customerID) < 2) {
                continue;
            }
            succeedingCustomer2 = trip2.customers.get(trip2.customerToTripIndexMap.get(neighbor.customerID) + 1);
            if (neighbor.customerID == succeedingCustomer1 || succeedingCustomer2 == customer || trip2.customers.size() - trip2.customerToTripIndexMap.get(neighbor.customerID) < 2) {
                continue;
            }
            originalTrip2 = new ArrayList<>(trip2.customers);
            if (fitnessDifferenceOfDoubleSwap(originalTrip1, originalTrip2, trip1, trip2, customer, succeedingCustomer1, neighbor.customerID, succeedingCustomer2) > 0) {
                performSwap(individual, trip1, trip2, customer, neighbor.customerID);
                performSwap(individual, trip1, trip2, succeedingCustomer1, succeedingCustomer2);
                return true;
            }
        }
        return false;
    }

    private static boolean twoOpt1(Individual individual, int customer, int period) {
        //if customer and neighbor is in the same route (trip), swap first successor with neighbor

        Trip trip1;
        Trip trip2;
        trip1 = individual.tripMap.get(period).get(customer);
        List<Integer> originalTrip1;
        List<Integer> originalTrip2;
        if (trip1.customers.size() - trip1.customerToTripIndexMap.get(customer) < 2 || trip1.customers.size() < 3) {
            return false;
        }
        originalTrip1 = new ArrayList<>(trip1.customers);

        int succeedingCustomer1 = trip1.customers.get(trip1.customerToTripIndexMap.get(customer) + 1);
        for (Customer neighbor : data.customers[customer].nearestNeighbors) {
            if (neighbor.requiredVisitPeriod[period] == 0) {
                continue;
            }
            trip2 = individual.tripMap.get(period).get(neighbor.customerID);
            if (trip1 == trip2 && neighbor.customerID != succeedingCustomer1) {
                originalTrip2 = new ArrayList<>(trip2.customers);
                if (fitnessDifferenceOfSwap(originalTrip1, originalTrip2, trip1, trip2, succeedingCustomer1, neighbor.customerID) > 0) {
                    performSwap(individual, trip1, trip2, succeedingCustomer1, neighbor.customerID);
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean twoOpt2(Individual individual, int customer, int period) {
        //if customer and neighbor does not come from same route (trip), replace successor with neighbor
        Trip trip1;
        Trip trip2;
        trip1 = individual.tripMap.get(period).get(customer);
        List<Integer> originalTrip1;
        List<Integer> originalTrip2;
        if (trip1.customers.size() - trip1.customerToTripIndexMap.get(customer) < 2 || trip1.customers.size() < 3) {
            return false;
        }
        originalTrip1 = new ArrayList<>(trip1.customers);

        int succeedingCustomer1 = trip1.customers.get(trip1.customerToTripIndexMap.get(customer) + 1);
        for (Customer neighbor : data.customers[customer].nearestNeighbors) {
            if (neighbor.requiredVisitPeriod[period] == 0) {
                continue;
            }
            trip2 = individual.tripMap.get(period).get(neighbor.customerID);
            if (trip1 != trip2 && neighbor.customerID != succeedingCustomer1) {
                originalTrip2 = new ArrayList<>(trip2.customers);
                if (fitnessDifferenceOfSwap(originalTrip1, originalTrip2, trip1, trip2, succeedingCustomer1, neighbor.customerID) > 0) {
                    performSwap(individual, trip1, trip2, succeedingCustomer1, neighbor.customerID);
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean twoOpt3(Individual individual, int customer, int period) {
        //if customer and neighbor not in same route (trip), swap customer - successor && neighbor - neighborSuccessor
        //with customer - neighborsuccessor && successor neighbor
        Trip trip1;
        Trip trip2;
        trip1 = individual.tripMap.get(period).get(customer);
        List<Integer> originalTrip1;
        List<Integer> originalTrip2;
        if (trip1.customers.size() - trip1.customerToTripIndexMap.get(customer) < 2 || trip1.customers.size() < 3) {
            return false;
        }
        originalTrip1 = new ArrayList<>(trip1.customers);
        int succeedingCustomer1 = trip1.customers.get(trip1.customerToTripIndexMap.get(customer) + 1);
        int succeedingCustomer2;
        for (Customer neighbor : data.customers[customer].nearestNeighbors) {
            if (neighbor.requiredVisitPeriod[period] == 0) {
                continue;
            }
            trip2 = individual.tripMap.get(period).get(neighbor.customerID);
            if (trip2.customers.size() - trip2.customerToTripIndexMap.get(neighbor.customerID) < 2) {
                continue;
            }
            if (trip1 != trip2 && neighbor.customerID != succeedingCustomer1) {
                originalTrip2 = new ArrayList<>(trip2.customers);
                succeedingCustomer2 = trip2.customers.get(trip2.customerToTripIndexMap.get(neighbor.customerID) + 1);
                if (fitnessDifferenceOf3WaySwap(originalTrip1, originalTrip2, trip1, trip2, succeedingCustomer1, neighbor.customerID, succeedingCustomer2) > 0) {
                    perform3WaySwap(individual, trip1, trip2, succeedingCustomer1, neighbor.customerID, succeedingCustomer2);
                    return true;
                }
            }
        }
        return false;

    }

    private static boolean doubleInsertion(Individual individual, int customer, int period, boolean reverse) {
        //inserts  customer and its successor after customers nearest neighbor if it gives improvement
        //If reverse is true, successor is placed prior to customer
        Trip trip1;
        Trip trip2;
        trip1 = individual.tripMap.get(period).get(customer);
        List<Integer> originalTrip1;
        List<Integer> originalTrip2;
        if (trip1.customers.size() - trip1.customerToTripIndexMap.get(customer) < 2) {
            return false;
        }
        originalTrip1 = new LinkedList<>(trip1.customers);
        int succeedingCustomer1 = trip1.customers.get(trip1.customerToTripIndexMap.get(customer) + 1);
        int c1;
        int c2;

        for (Customer neighbor : data.customers[customer].nearestNeighbors) {
            if (neighbor.requiredVisitPeriod[period] == 0 || neighbor.customerID == succeedingCustomer1) {
                continue;
            }
            trip2 = individual.tripMap.get(period).get(neighbor.customerID);
            originalTrip2 = new LinkedList<>(trip2.customers);
            c1 = reverse ? succeedingCustomer1 : customer;
            c2 = reverse ? customer : succeedingCustomer1;
            if (fitnessDifferenceOfDoubleInsertionRemoval(originalTrip1, originalTrip2, trip1, trip2, c1, c2, neighbor.customerID, reverse) > 0) {
                doRemovalInsertion(individual, trip1, trip2, c1, neighbor.customerID);
                doRemovalInsertion(individual, trip1, trip2, c2, c1);
                return true;
            }
        }
        return false;
    }

//  ------------------------------HELPER FUNCTIONS-------------------------------------------------------

    private static double fitnessDifferenceOfRemovalInsertion(List<Integer> customerSequence1, List<Integer> customerSequence2, Trip trip1, Trip trip2, int customer1, int customer2) {
        double oldFitness = combinedFitnessOf2Sequences(customerSequence1, customerSequence2, trip1.vehicleType, trip1.period, trip2.vehicleType, trip2.period);
        int indexCustomer1 = trip1.customerToTripIndexMap.get(customer1);
        int indexCustomer2 = trip2.customerToTripIndexMap.get(customer2);
        int adjustment = 1;
        if (trip1 == trip2) {
            customerSequence2 = customerSequence1;
            if (indexCustomer1 < indexCustomer2) {
                adjustment = 0;
            }
        }
        customerSequence1.remove(indexCustomer1);
        customerSequence2.add(indexCustomer2 + adjustment, customer1);
        double newFitness = combinedFitnessOf2Sequences(customerSequence1, customerSequence2, trip1.vehicleType, trip1.period, trip2.vehicleType, trip2.period);
        //set back to normal
        customerSequence2.remove(indexCustomer2 + adjustment);
        customerSequence1.add(indexCustomer1, customer1);
        return oldFitness - newFitness;
    }


    private static double fitnessDifferenceOfDoubleInsertionRemoval(List<Integer> customerSequence1, List<Integer> customerSequence2, Trip trip1, Trip trip2, int insertedCustomer1, int insertedCustomer2, int targetCustomer, boolean reverse) {
        double oldFitness = combinedFitnessOf2Sequences(customerSequence1, customerSequence2, trip1.vehicleType, trip1.period, trip2.vehicleType, trip2.period);
        int indexCustomer1 = Math.min(trip1.customerToTripIndexMap.get(insertedCustomer1), trip1.customerToTripIndexMap.get(insertedCustomer2));
        int indexCustomer3 = trip2.customerToTripIndexMap.get(targetCustomer);
        int adjustment = 1;
        if (trip1 == trip2) {
            if (indexCustomer3 + 1 == indexCustomer1) {
                return -1;
            }
            customerSequence2 = customerSequence1;
            if (indexCustomer1 < indexCustomer3) {
                adjustment = -1;
            }
        }
        customerSequence1.remove(indexCustomer1);
        customerSequence1.remove(indexCustomer1);
        customerSequence2.add(indexCustomer3 + adjustment, insertedCustomer2);
        customerSequence2.add(indexCustomer3 + adjustment, insertedCustomer1);
        double newFitness = combinedFitnessOf2Sequences(customerSequence1, customerSequence2, trip1.vehicleType, trip1.period, trip2.vehicleType, trip2.period);
        //back to original
        customerSequence2.remove(indexCustomer3 + adjustment);
        customerSequence2.remove(indexCustomer3 + adjustment);
        if (reverse) {
            customerSequence1.add(indexCustomer1, insertedCustomer1);
            customerSequence1.add(indexCustomer1, insertedCustomer2);
        } else {
            customerSequence1.add(indexCustomer1, insertedCustomer2);
            customerSequence1.add(indexCustomer1, insertedCustomer1);
        }
        return oldFitness - newFitness;
    }

    private static double fitnessDifferenceOfSwap(List<Integer> customerSequence1, List<Integer> customerSequence2, Trip trip1, Trip trip2, int customer1, int customer2) {
        double oldFitness = combinedFitnessOf2Sequences(customerSequence1, customerSequence2, trip1.vehicleType, trip1.period, trip2.vehicleType, trip2.period);
        //perform swap and check new fitness
        if (trip1 == trip2) {
            customerSequence2 = customerSequence1;
        }
        int index1 = trip1.customerToTripIndexMap.get(customer1);
        int index2 = trip2.customerToTripIndexMap.get(customer2);
        customerSequence1.set(index1, customer2);
        customerSequence2.set(index2, customer1);
        double newFitness = combinedFitnessOf2Sequences(customerSequence1, customerSequence2, trip1.vehicleType, trip1.period, trip2.vehicleType, trip2.period);
        //swap back to original
        customerSequence1.set(index1, customer1);
        customerSequence2.set(index2, customer2);
        return oldFitness - newFitness;
    }

    private static double fitnessDifferenceOfDoubleSwap(List<Integer> customerSequence1, List<Integer> customerSequence2, Trip trip1, Trip trip2, int customer1, int succeedingCustomer1, int customer2, int succeedingCustomer2) {
        double oldFitness = combinedFitnessOf2Sequences(customerSequence1, customerSequence2, trip1.vehicleType, trip1.period, trip2.vehicleType, trip2.period);
        int indexCustomer1 = trip1.customerToTripIndexMap.get(customer1);
        int indexCustomer2 = trip2.customerToTripIndexMap.get(customer2);
        int indexSucceeding1 = trip1.customerToTripIndexMap.get(succeedingCustomer1);
        int indexSucceeding2 = trip2.customerToTripIndexMap.get(succeedingCustomer2);
        if (trip1 == trip2) {
            customerSequence1 = customerSequence2;
        }
        customerSequence1.set(indexCustomer1, customer2);
        customerSequence2.set(indexCustomer2, customer1);
        customerSequence1.set(indexSucceeding1, succeedingCustomer2);
        customerSequence2.set(indexSucceeding2, succeedingCustomer1);
        double newFitness = combinedFitnessOf2Sequences(customerSequence1, customerSequence2, trip1.vehicleType, trip1.period, trip2.vehicleType, trip2.period);
        //swap back to original
        customerSequence1.set(indexCustomer1, customer1);
        customerSequence2.set(indexCustomer2, customer2);
        customerSequence1.set(indexSucceeding1, succeedingCustomer1);
        customerSequence2.set(indexSucceeding2, succeedingCustomer2);
        return oldFitness - newFitness;
    }

    private static double fitnessDifferenceOf3WaySwap(List<Integer> customerSequence1, List<Integer> customerSequence2, Trip trip1, Trip trip2, int succeedingCustomer1, int customer2, int succeedingCustomer2) {
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
        return oldFitness - newFitness;

    }

    private static double fitnessDifferenceOfSwapAndInsertion(List<Integer> customerSequence1, List<Integer> customerSequence2, Trip trip1, Trip trip2, int customer1, int succeedingCustomer1, int customer2) {
        double oldFitness = combinedFitnessOf2Sequences(customerSequence1, customerSequence2, trip1.vehicleType, trip1.period, trip2.vehicleType, trip2.period);
        int adjustment = 1;
        int indexCustomer1 = trip1.customerToTripIndexMap.get(customer1);
        int indexCustomer2 = trip2.customerToTripIndexMap.get(customer2);
        int indexSucceeding1 = trip1.customerToTripIndexMap.get(succeedingCustomer1);
        if (trip1 == trip2) {
            customerSequence2 = customerSequence1;
            if (indexCustomer1 < indexCustomer2) {
                adjustment = 0;
            }
        }
        customerSequence1.set(indexCustomer1, customer2);
        customerSequence2.set(indexCustomer2, customer1);
        customerSequence1.remove(indexSucceeding1);
        customerSequence2.add(indexCustomer2 + adjustment, succeedingCustomer1);
        double newFitness = combinedFitnessOf2Sequences(customerSequence1, customerSequence2, trip1.vehicleType, trip1.period, trip2.vehicleType, trip2.period);
        customerSequence2.remove(indexCustomer2 + adjustment);
        customerSequence1.add(indexSucceeding1, succeedingCustomer1);
        customerSequence2.set(indexCustomer2, customer2);
        customerSequence1.set(indexCustomer1, customer1);
        return oldFitness - newFitness;

    }

    private static double combinedFitnessOf2Sequences(List<Integer> customerSequence1, List<Integer> customerSequence2, int vt1, int period1, int vt2, int period2) {
        return FitnessCalculation.getTripFitness(customerSequence1, vt1, period1, orderDistribution.orderVolumeDistribution, data)
                + FitnessCalculation.getTripFitness(customerSequence2, vt2, period2, orderDistribution.orderVolumeDistribution, data);
    }

    private static void doRemovalInsertion(Individual individual, Trip trip1, Trip trip2, int removeCustomer, int targetCustomer) {
        trip1.removeCustomer(removeCustomer);
        trip2.addCustomer(removeCustomer, trip2.customerToTripIndexMap.get(targetCustomer) + 1);
        individual.tripMap.get(trip2.period).put(removeCustomer, trip2);
    }


    private static void perform3WaySwap(Individual individual, Trip trip1, Trip trip2, int succeedingCustomer1, int customer2, int succeedingCustomer2) {
        int setIndex1 = trip1.customerToTripIndexMap.get(succeedingCustomer1);
        int setIndex2 = trip2.customerToTripIndexMap.get(customer2);
        trip1.setCustomer(succeedingCustomer2, setIndex1);
        trip2.setCustomer(succeedingCustomer1, setIndex2);
        trip2.setCustomer(customer2, setIndex2 + 1);
        individual.tripMap.get(trip2.period).put(succeedingCustomer1, trip2);
        individual.tripMap.get(trip1.period).put(succeedingCustomer2, trip1);
    }

    private static void performSwap(Individual individual, Trip trip1, Trip trip2, int customer1, int customer2) {
        int setIndex1 = trip1.customerToTripIndexMap.get(customer1);
        int setIndex2 = trip2.customerToTripIndexMap.get(customer2);
        trip1.setCustomer(customer2, setIndex1);
        trip2.setCustomer(customer1, setIndex2);
        individual.tripMap.get(trip1.period).put(customer1, trip2);
        individual.tripMap.get(trip2.period).put(customer2, trip1);
    }


    public static void main(String[] args) {
        Data data = DataReader.loadData();
        Population population = new Population(data);
        OrderDistributionPopulation odp = new OrderDistributionPopulation(data);
        OrderDistributionCrossover ODC = new OrderDistributionCrossover(data);
        odp.initializeOrderDistributionPopulation(population);
        OrderDistribution firstOD = odp.getRandomOrderDistribution();
        population.setOrderDistributionPopulation(odp);
        population.initializePopulation(firstOD);

        for (int i = 0; i < 1; i++) {
            Individual individual = population.getRandomIndividual();
            Education.improveRoutes(individual, individual.orderDistribution);

        }
    }
}

