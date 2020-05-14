package Master2020.Genetic;

import Master2020.DataFiles.*;
import Master2020.Individual.*;
import Master2020.Population.Population;
import Master2020.ProductAllocation.OrderDistribution;
import Master2020.Population.OrderDistributionPopulation;

import java.util.*;

public class Education {


    public static Data data;
    public static OrderDistribution orderDistribution;
    public static double penaltyMultiplier;

    public static void improveRoutes(Individual individual, OrderDistribution od, double timeWarpPenalty, double overLoadPenalty) {
        improveRoutes(individual, od, 1, timeWarpPenalty, overLoadPenalty );
    }

    public static void improveRoutes(Individual individual, OrderDistribution od, double pm, double timeWarpPenalty, double overLoadPenalty) {
        data = individual.data;
        orderDistribution = od;
        penaltyMultiplier = pm;
        List<Integer> customers = new ArrayList<>();
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
        int totalImprovements = 0;
        Collections.shuffle(customers);
        for (int c : customers) {
            taboo = new int[Parameters.educationTabooSize];
            for (int period = 0; period < individual.numberOfPeriods; period++) {
                if (data.customers[c].requiredVisitPeriod[individual.getActualPeriod(period)] == 0) {
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
                                    index = (index + 1) % Parameters.educationTabooSize;
                                }
                                break;
                            case 1:
                                temp = insert2(individual, c, period);
                                improved = improved || temp;
                                if (temp) {
                                    totalImprovements++;
                                    taboo[index] = move;
                                    index = (index + 1) % Parameters.educationTabooSize;
                                }
                                break;
                            case 2:
                                temp = insert3(individual, c, period);
                                improved = improved || temp;
                                if (temp) {
                                    totalImprovements++;
                                    taboo[index] = move;
                                    index = (index + 1) % Parameters.educationTabooSize;
                                }
                                break;
                            case 3:
                                temp = swap1(individual, c, period);
                                improved = improved || temp;
                                if (temp) {
                                    totalImprovements++;
                                    taboo[index] = move;
                                    index = (index + 1) % Parameters.educationTabooSize;
                                }
                                break;
                            case 4:
                                temp = swap2(individual, c, period);
                                improved = improved || temp;
                                if (temp) {
                                    totalImprovements++;
                                    taboo[index] = move;
                                    index = (index + 1) % Parameters.educationTabooSize;
                                }
                                break;
                            case 5:
                                temp = swap3(individual, c, period);
                                improved = improved || temp;
                                if (temp) {
                                    totalImprovements++;
                                    taboo[index] = move;
                                    index = (index + 1) % Parameters.educationTabooSize;
                                }
                                break;
                            case 6:
                                temp = twoOpt1(individual, c, period);
                                improved = improved || temp;
                                if (temp) {
                                    totalImprovements++;
                                    taboo[index] = move;
                                    index = (index + 1) % Parameters.educationTabooSize;
                                }
                                break;
                            case 7:
                                temp = twoOpt2(individual, c, period);
                                improved = improved || temp;
                                if (temp) {
                                    totalImprovements++;
                                    taboo[index] = move;
                                    index = (index + 1) % Parameters.educationTabooSize;
                                }
                                break;
                            case 8:
                                temp = twoOpt3(individual, c, period);
                                improved = improved || temp;
                                if (temp) {
                                    totalImprovements++;
                                    taboo[index] = move;
                                    index = (index + 1) % Parameters.educationTabooSize;
                                }
                                break;
                        }
                    }
                }
            }
        }
        if (Parameters.verbose) {
            System.out.println("total totalImprovements in Education: " + totalImprovements);
        }
        individual.setGiantTourFromTrips();
        AdSplit.adSplitPlural(individual, penaltyMultiplier, timeWarpPenalty, overLoadPenalty);
    }


    private static boolean insert1(Individual individual, int customer, int period) {
        //Tries to remove a single customer from its trip, and insert it after one of its nearest neighbors
        Trip trip1;
        Trip trip2;
        trip1 = individual.tripMap.get(period).get(customer);
        for (Customer neighbor : data.customers[customer].nearestNeighbors) {
            if (neighbor.requiredVisitPeriod[individual.getActualPeriod(period)] == 0) {
                continue;
            }
            trip2 = individual.tripMap.get(period).get(neighbor.customerID);
            if (fitnessDifferenceOfRemovalInsertion(trip1, trip2, customer, neighbor.customerID) > 0) {
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
        for (Customer neighbor : data.customers[customer].nearestNeighbors) {
            if (neighbor.requiredVisitPeriod[individual.getActualPeriod(period)] == 0) {
                continue;
            }

            trip2 = individual.tripMap.get(period).get(neighbor.customerID);
            if (fitnessDifferenceOfSwap(trip1, trip2, customer, neighbor.customerID) > 0) {
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
        if (trip1 == null){
            System.out.println("check trip");
        }
        if (trip1.customers.size() - trip1.customerToTripIndexMap.get(customer) < 2) {
            return false;
        }
        int succeedingCustomer = trip1.customers.get(trip1.customerToTripIndexMap.get(customer) + 1);

        double fitnessDifferenceOfSwap;
        for (Customer neighbor : data.customers[customer].nearestNeighbors) {
            if (neighbor.requiredVisitPeriod[individual.getActualPeriod(period)] == 0 || neighbor.customerID == succeedingCustomer) {
                continue;
            }
            trip2 = individual.tripMap.get(period).get(neighbor.customerID);
            fitnessDifferenceOfSwap = fitnessDifferenceOfSwapAndInsertion(trip1, trip2, customer, succeedingCustomer, neighbor.customerID);
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
        if(trip1 == null){
            System.out.println("Check if trip exists");
        }

        if (trip1.customers.size() - trip1.customerToTripIndexMap.get(customer) < 2) {
            return false;
        }
        int succeedingCustomer1 = trip1.customers.get(trip1.customerToTripIndexMap.get(customer) + 1);
        int succeedingCustomer2;
        for (Customer neighbor : data.customers[customer].nearestNeighbors) {
            if (neighbor.requiredVisitPeriod[individual.getActualPeriod(period)] == 0) {
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
            if (fitnessDifferenceOfDoubleSwap(trip1, trip2, customer, succeedingCustomer1, neighbor.customerID, succeedingCustomer2) > 0) {
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
        if (trip1.customers.size() - trip1.customerToTripIndexMap.get(customer) < 2 || trip1.customers.size() < 3) {
            return false;
        }
        int succeedingCustomer1 = trip1.customers.get(trip1.customerToTripIndexMap.get(customer) + 1);
        for (Customer neighbor : data.customers[customer].nearestNeighbors) {
            if (neighbor.requiredVisitPeriod[individual.getActualPeriod(period)] == 0) {
                continue;
            }
            trip2 = individual.tripMap.get(period).get(neighbor.customerID);
            if (trip1 == trip2 && neighbor.customerID != succeedingCustomer1) {
                if (fitnessDifferenceOfSwap(trip1, trip2, succeedingCustomer1, neighbor.customerID) > 0) {
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
        if (trip1 == null ){
            System.out.println("Check trip");
        }
        if (trip1.customers.size() - trip1.customerToTripIndexMap.get(customer) < 2 || trip1.customers.size() < 3) {
            return false;
        }
        int succeedingCustomer1 = trip1.customers.get(trip1.customerToTripIndexMap.get(customer) + 1);
        for (Customer neighbor : data.customers[customer].nearestNeighbors) {
            if (neighbor.requiredVisitPeriod[individual.getActualPeriod(period)] == 0) {
                continue;
            }
            trip2 = individual.tripMap.get(period).get(neighbor.customerID);
            if (trip1 != trip2 && neighbor.customerID != succeedingCustomer1) {
                if (fitnessDifferenceOfSwap(trip1, trip2, succeedingCustomer1, neighbor.customerID) > 0) {
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
        if (trip1 == null){
            System.out.println("Check trip");
        }

        if (trip1.customers.size() - trip1.customerToTripIndexMap.get(customer) < 2 || trip1.customers.size() < 3) {
            return false;
        }
        int succeedingCustomer1 = trip1.customers.get(trip1.customerToTripIndexMap.get(customer) + 1);
        int succeedingCustomer2;
        for (Customer neighbor : data.customers[customer].nearestNeighbors) {
            if (neighbor.requiredVisitPeriod[individual.getActualPeriod(period)] == 0) {
                continue;
            }
            trip2 = individual.tripMap.get(period).get(neighbor.customerID);
            if (trip2.customers.size() - trip2.customerToTripIndexMap.get(neighbor.customerID) < 2) {
                continue;
            }
            if (trip1 != trip2 && neighbor.customerID != succeedingCustomer1) {
                succeedingCustomer2 = trip2.customers.get(trip2.customerToTripIndexMap.get(neighbor.customerID) + 1);
                if (fitnessDifferenceOf3WaySwap(trip1, trip2, succeedingCustomer1, neighbor.customerID, succeedingCustomer2) > 0) {
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
        if (trip1 == null){
            System.out.println();
        }
        if (trip1.customers.size() - trip1.customerToTripIndexMap.get(customer) < 2) {
            return false;
        }
        int succeedingCustomer1 = trip1.customers.get(trip1.customerToTripIndexMap.get(customer) + 1);
        int c1;
        int c2;
        for (Customer neighbor : data.customers[customer].nearestNeighbors) {
            if (neighbor.requiredVisitPeriod[individual.getActualPeriod(period)] == 0 || neighbor.customerID == succeedingCustomer1) {
                continue;
            }
            trip2 = individual.tripMap.get(period).get(neighbor.customerID);
            c1 = reverse ? succeedingCustomer1 : customer;
            c2 = reverse ? customer : succeedingCustomer1;
            if (fitnessDifferenceOfDoubleInsertionRemoval(trip1, trip2, c1, c2, neighbor.customerID, reverse) > 0) {
                doRemovalInsertion(individual, trip1, trip2, c1, neighbor.customerID);
                doRemovalInsertion(individual, trip1, trip2, c2, c1);
                return true;
            }
        }
        return false;
    }

//  ------------------------------HELPER FUNCTIONS-------------------------------------------------------

    private static double fitnessDifferenceOfRemovalInsertion(Trip trip1, Trip trip2, int customer1, int customer2) {
        double oldFitness = combinedFitnessOf2Sequences(trip1.journey, trip2.journey);
        int indexCustomer1 = trip1.customerToTripIndexMap.get(customer1);
        int indexCustomer2 = trip2.customerToTripIndexMap.get(customer2);
        int adjustment = 1;
        if (trip1 == trip2) {
            trip2 = trip1;
            if (indexCustomer1 < indexCustomer2) {
                adjustment = 0;
            }
        }
        trip1.customers.remove(indexCustomer1);
        trip2.customers.add(indexCustomer2 + adjustment, customer1);
        double newFitness = combinedFitnessOf2Sequences(trip1.journey, trip2.journey);
        //set back to normal
        trip2.customers.remove(indexCustomer2 + adjustment);
        trip1.customers.add(indexCustomer1, customer1);
        return oldFitness - newFitness;
    }


    private static double fitnessDifferenceOfDoubleInsertionRemoval(Trip trip1, Trip trip2, int insertedCustomer1, int insertedCustomer2, int targetCustomer, boolean reverse) {
        double oldFitness = combinedFitnessOf2Sequences(trip1.journey, trip2.journey);
        int indexCustomer1 = Math.min(trip1.customerToTripIndexMap.get(insertedCustomer1), trip1.customerToTripIndexMap.get(insertedCustomer2));
        int indexCustomer3 = trip2.customerToTripIndexMap.get(targetCustomer);
        int adjustment = 1;
        if (trip1 == trip2) {
            if (indexCustomer3 + 1 == indexCustomer1) {
                return -1;
            }
            trip2 = trip1;
            if (indexCustomer1 < indexCustomer3) {
                adjustment = -1;
            }
        }
        trip1.customers.remove(indexCustomer1);
        trip1.customers.remove(indexCustomer1);
        trip2.customers.add(indexCustomer3 + adjustment, insertedCustomer2);
        trip2.customers.add(indexCustomer3 + adjustment, insertedCustomer1);
        double newFitness = combinedFitnessOf2Sequences(trip1.journey, trip2.journey);
        //back to original
        trip2.customers.remove(indexCustomer3 + adjustment);
        trip2.customers.remove(indexCustomer3 + adjustment);
        if (reverse) {
            trip1.customers.add(indexCustomer1, insertedCustomer1);
            trip1.customers.add(indexCustomer1, insertedCustomer2);
        } else {
            trip1.customers.add(indexCustomer1, insertedCustomer2);
            trip1.customers.add(indexCustomer1, insertedCustomer1);
        }
        return oldFitness - newFitness;
    }

    private static double fitnessDifferenceOfSwap(Trip trip1, Trip trip2, int customer1, int customer2) {
        double oldFitness = combinedFitnessOf2Sequences(trip1.journey, trip2.journey);
        //perform swap and check new fitness
        if (trip1 == trip2) {
            trip2 = trip1;
        }
        int index1 = trip1.customerToTripIndexMap.get(customer1);
        int index2 = trip2.customerToTripIndexMap.get(customer2);
        trip1.customers.set(index1, customer2);
        trip2.customers.set(index2, customer1);
        double newFitness = combinedFitnessOf2Sequences(trip1.journey, trip2.journey);
        //swap back to original
        trip1.customers.set(index1, customer1);
        trip2.customers.set(index2, customer2);
        return oldFitness - newFitness;
    }

    private static double fitnessDifferenceOfDoubleSwap(Trip trip1, Trip trip2, int customer1, int succeedingCustomer1, int customer2, int succeedingCustomer2) {
        double oldFitness = combinedFitnessOf2Sequences(trip1.journey, trip2.journey);
        int indexCustomer1 = trip1.customerToTripIndexMap.get(customer1);
        int indexCustomer2 = trip2.customerToTripIndexMap.get(customer2);
        int indexSucceeding1 = trip1.customerToTripIndexMap.get(succeedingCustomer1);
        int indexSucceeding2 = trip2.customerToTripIndexMap.get(succeedingCustomer2);
        if (trip1 == trip2) {
            trip2 = trip1;
        }
        trip1.customers.set(indexCustomer1, customer2);
        trip2.customers.set(indexCustomer2, customer1);
        trip1.customers.set(indexSucceeding1, succeedingCustomer2);
        trip2.customers.set(indexSucceeding2, succeedingCustomer1);
        double newFitness = combinedFitnessOf2Sequences(trip1.journey, trip2.journey);
        //swap back to original
        trip1.customers.set(indexCustomer1, customer1);
        trip2.customers.set(indexCustomer2, customer2);
        trip1.customers.set(indexSucceeding1, succeedingCustomer1);
        trip2.customers.set(indexSucceeding2, succeedingCustomer2);
        return oldFitness - newFitness;
    }

    private static double fitnessDifferenceOf3WaySwap(Trip trip1, Trip trip2, int succeedingCustomer1, int customer2, int succeedingCustomer2) {
        //swaps ctt1 into ctt2, ctt2 into ctt3, and ctt3 into ctt1
        double oldFitness = combinedFitnessOf2Sequences(trip1.journey, trip2.journey);
        trip1.customers.set(trip1.customerToTripIndexMap.get(succeedingCustomer1), succeedingCustomer2);
        trip2.customers.set(trip2.customerToTripIndexMap.get(customer2), succeedingCustomer1);
        trip2.customers.set(trip2.customerToTripIndexMap.get(succeedingCustomer2), customer2);
        double newFitness = combinedFitnessOf2Sequences(trip1.journey, trip2.journey);
        //set back to original
        trip1.customers.set(trip1.customerToTripIndexMap.get(succeedingCustomer1), succeedingCustomer1);
        trip2.customers.set(trip2.customerToTripIndexMap.get(customer2), customer2);
        trip2.customers.set(trip2.customerToTripIndexMap.get(succeedingCustomer2), succeedingCustomer2);
        return oldFitness - newFitness;

    }

    private static double fitnessDifferenceOfSwapAndInsertion(Trip trip1, Trip trip2, int customer1, int succeedingCustomer1, int customer2) {
        double oldFitness = combinedFitnessOf2Sequences(trip1.journey, trip2.journey);
        int adjustment = 1;
        int indexCustomer1 = trip1.customerToTripIndexMap.get(customer1);
        int indexCustomer2 = trip2.customerToTripIndexMap.get(customer2);
        int indexSucceeding1 = trip1.customerToTripIndexMap.get(succeedingCustomer1);
        if (trip1 == trip2) {
            trip2 = trip1;
            if (indexCustomer1 < indexCustomer2) {
                adjustment = 0;
            }
        }
        trip1.customers.set(indexCustomer1, customer2);
        trip2.customers.set(indexCustomer2, customer1);
        trip1.customers.remove(indexSucceeding1);
        trip2.customers.add(indexCustomer2 + adjustment, succeedingCustomer1);
        double newFitness = combinedFitnessOf2Sequences(trip1.journey, trip2.journey);
        //set back to normal
        trip2.customers.remove(indexCustomer2 + adjustment);
        trip1.customers.add(indexSucceeding1, succeedingCustomer1);
        trip2.customers.set(indexCustomer2, customer2);
        trip1.customers.set(indexCustomer1, customer1);
        return oldFitness - newFitness;

    }

    private static double combinedFitnessOf2Sequences(Journey journey1, Journey journey2) {
        return FitnessCalculation.getTotalJourneyFitness(journey1, orderDistribution) + FitnessCalculation.getTotalJourneyFitness(journey2, orderDistribution);
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
        population.initializePopulation(firstOD, Parameters.initialTimeWarpPenalty, Parameters.initialOverLoadPenalty);

        for (int i = 0; i < 1; i++) {
            Individual individual = population.getRandomIndividual();
            Education.improveRoutes(individual, individual.orderDistribution, Parameters.initialTimeWarpPenalty, Parameters.initialOverLoadPenalty);

        }
    }
}



