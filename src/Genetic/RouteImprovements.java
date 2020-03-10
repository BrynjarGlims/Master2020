package Genetic;

import DataFiles.Customer;
import DataFiles.CustomerToTrip;
import DataFiles.Data;
import DataFiles.DataReader;
import Individual.*;
import ProductAllocation.OrderDistribution;

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
        List<Integer> originalTrip1;
        List<Integer> originalTrip2;
        CustomerToTrip ctt1 = individual.customerToTrips[period][customer];
        CustomerToTrip ctt2;

        originalTrip1 = new LinkedList<>(individual.giantTour.chromosome[period][ctt1.vehicletype].subList(ctt1.startIndex, ctt1.endIndex));
        System.out.println("original trip 1: " + originalTrip1);

        improvementOfRemoval = fitnessDifferenceOfRemoval(originalTrip1, ctt1);
        int carsInUsePrior = individual.bestLabels[period][ctt1.vehicletype].getNumberOfVehicles();
        for (Customer neighbor : data.customers[customer].nearestNeighbors){
            if (neighbor.requiredVisitPeriod[period] == 0){
                continue;
            }
            ctt2 = individual.customerToTrips[period][neighbor.customerID];
            originalTrip2 = new LinkedList<>(individual.giantTour.chromosome[period][ctt2.vehicletype].subList(ctt2.startIndex, ctt2.endIndex));

            if (ctt2.vehicletype == ctt1.vehicletype && ctt1.startIndex == ctt2.startIndex){
                System.out.println("removing customer");
                System.out.println(originalTrip2);
                originalTrip2.remove(ctt1.index);
                System.out.println(originalTrip2);
            }

            System.out.println("original trip 2: " + originalTrip2);
            System.out.println("neighbor: " + ctt2.customer);
            detoriorationOfInsertion = fitnessDifferenceOfInsertion(originalTrip2, ctt2, customer);
            if (improvementOfRemoval > detoriorationOfInsertion){
                System.out.println("improvement: " + improvementOfRemoval);
                System.out.println("detorioration: " + detoriorationOfInsertion);
                System.out.println("current customer: " + ctt1.customer + " placed after: " + ctt2.customer);
                System.out.println("old chromosome c1: " + individual.giantTour.chromosome[period][ctt1.vehicletype]);
                System.out.println("old chromosome c1: " + individual.giantTour.chromosome[period][ctt2.vehicletype]);
                double total = improvementOfRemoval - detoriorationOfInsertion;
                carsInUsePrior += individual.bestLabels[period][ctt2.vehicletype].getNumberOfVehicles();
                doRemovalInsertion(individual, ctt1, ctt2);


                System.out.println("new chromosome c1: " + individual.giantTour.chromosome[period][ctt1.vehicletype]);
                System.out.println("new chromosome c1: " + individual.giantTour.chromosome[period][ctt2.vehicletype]);
                int carsInUsePost = individual.bestLabels[period][ctt1.vehicletype].getNumberOfVehicles();
                carsInUsePost += individual.bestLabels[period][ctt2.vehicletype].getNumberOfVehicles();
                ctt2 = individual.customerToTrips[period][customer];
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
        List<Integer> originalTrip1;
        List<Integer> originalTrip2;
        CustomerToTrip ctt1 = individual.customerToTrips[period][customer];

        originalTrip1 = new ArrayList<>(individual.giantTour.chromosome[period][ctt1.vehicletype].subList(ctt1.startIndex, ctt1.endIndex));
        CustomerToTrip ctt2;
        for (Customer neighbor : data.customers[customer].nearestNeighbors) {
            if (neighbor.requiredVisitPeriod[period] == 0) {
                continue;
            }
            ctt2 = individual.customerToTrips[period][neighbor.customerID];
            originalTrip2 = new ArrayList<>(individual.giantTour.chromosome[period][ctt2.vehicletype].subList(ctt2.startIndex, ctt2.endIndex));
            if (fitnessDifferenceOfSwap(originalTrip1, originalTrip2, ctt1, ctt2) > 0){
                performSwap(individual, ctt1, ctt2);
                return true;
            }
        }
        return false;
    }

    private static boolean swap2(Individual individual, int customer, int period){
        CustomerToTrip ctt1 = individual.customerToTrips[period][customer];
        if (ctt1.endIndex - (ctt1.startIndex + ctt1.index) < 2){
            return false;
        }
        List<Integer> originalTrip1;
        List<Integer> originalTrip2;
        originalTrip1 = new LinkedList<>(individual.giantTour.chromosome[period][ctt1.vehicletype].subList(ctt1.startIndex, ctt1.endIndex));
        int succeedingCustomer = getSucceedingCustomer(individual, ctt1);
        CustomerToTrip cttSucceeding = individual.customerToTrips[period][succeedingCustomer];

        CustomerToTrip ctt2;
        double fitnessDifferenceOfSwap;
        for (Customer neighbor : data.customers[customer].nearestNeighbors) {
            if (neighbor.requiredVisitPeriod[period] == 0 || neighbor.customerID == succeedingCustomer) {
                continue;
            }
            ctt2 = individual.customerToTrips[period][neighbor.customerID];
            originalTrip2 = new LinkedList<>(individual.giantTour.chromosome[period][ctt2.vehicletype].subList(ctt2.startIndex, ctt2.endIndex));
            originalTrip2.add(ctt2.index + 1, succeedingCustomer);
            fitnessDifferenceOfSwap = fitnessDifferenceOfSwapAndInsertion(originalTrip1, originalTrip2, ctt1, ctt2, cttSucceeding);
            if (fitnessDifferenceOfSwap > 0){
//                System.out.println("fitness improvement move 4: " + fitnessDifferenceOfSwap);
//                System.out.println("current customer: " + ctt1.customer + " placed after: " + ctt2.customer);
//                System.out.println("old chromosome c1: " + individual.giantTour.chromosome[period][ctt1.vehicletype]);
//                System.out.println("old chromosome c2: " + individual.giantTour.chromosome[period][ctt2.vehicletype]);
//                System.out.println("old chromosome c1 GTS: " + individual.giantTourSplit.chromosome[period][ctt1.vehicletype]);
//                System.out.println("old chromosome c2 GTS: " + individual.giantTourSplit.chromosome[period][ctt2.vehicletype]);
                performSwap(individual, ctt1, ctt2, false);
                doRemovalInsertion(individual, cttSucceeding, ctt2);
//                System.out.println("new chromosome c1: " + individual.giantTour.chromosome[period][ctt1.vehicletype]);
//                System.out.println("new chromosome c2: " + individual.giantTour.chromosome[period][ctt2.vehicletype]);
//                System.out.println("new chromosome c1 GTS: " + individual.giantTourSplit.chromosome[period][ctt1.vehicletype]);
//                System.out.println("mew chromosome c2 GTS: " + individual.giantTourSplit.chromosome[period][ctt2.vehicletype]);
                return true;
            }
        }
        return false;
    }

    private static boolean swap3(Individual individual, int customer, int period){
        //swaps chosen customer and successor with neighbor and successor
        CustomerToTrip ctt1 = individual.customerToTrips[period][customer];
        if (ctt1.endIndex - (ctt1.startIndex + ctt1.index) < 2){
            return false;
        }
        List<Integer> originalTrip1 = new ArrayList<>(individual.giantTour.chromosome[period][ctt1.vehicletype].subList(ctt1.startIndex, ctt1.endIndex));
        CustomerToTrip ctt1Successor = individual.customerToTrips[period][ getSucceedingCustomer(individual, ctt1)];

        List<Integer> originalTrip2;
        CustomerToTrip ctt2;
        CustomerToTrip ctt2Successor;
        for (Customer neighbor : data.customers[customer].nearestNeighbors) {
            ctt2 = individual.customerToTrips[period][neighbor.customerID];
            if (neighbor.requiredVisitPeriod[period] == 0 || neighbor.customerID == ctt1Successor.customer || ctt2.endIndex - (ctt2.startIndex + ctt2.index) < 2) {
                continue;
            }
            ctt2 = individual.customerToTrips[period][neighbor.customerID];
            ctt2Successor = individual.customerToTrips[period][ getSucceedingCustomer(individual, ctt2)];
            originalTrip2 = new ArrayList<>(individual.giantTour.chromosome[period][ctt2.vehicletype].subList(ctt2.startIndex, ctt2.endIndex));

            double fitness = fitnessDifferenceOfDoubleSwap(originalTrip1, originalTrip2, ctt1, ctt1Successor, ctt2, ctt2Successor);
            if (fitness > 0){
                performSwap(individual, ctt1, ctt2, false);
                performSwap(individual, ctt1Successor, ctt2Successor);
                return true;
            }
        }
        return false;
    }

    private static boolean twoOpt1(Individual individual, int customer, int period){
        //if customer and neighbor is in the same route (trip), swap first successor with neighbor

        CustomerToTrip ctt1 = individual.customerToTrips[period][customer];
        if (ctt1.endIndex - (ctt1.startIndex + ctt1.index) < 2 || ctt1.endIndex - ctt1.startIndex < 4){
            return false;
        }
        List<Integer> originalTrip1 = new ArrayList<>(individual.giantTour.chromosome[period][ctt1.vehicletype].subList(ctt1.startIndex, ctt1.endIndex));

        CustomerToTrip ctt1Successor = individual.customerToTrips[period][getSucceedingCustomer(individual, ctt1)];
        CustomerToTrip ctt2;
        List<Integer> originalTrip2;
        for (Customer neighbor : data.customers[customer].nearestNeighbors) {
            if (neighbor.requiredVisitPeriod[period] == 0){
                continue;
            }
            ctt2 = individual.customerToTrips[period][neighbor.customerID];
            if (ctt2.endIndex - (ctt2.startIndex + ctt2.index) < 2 && isValidSameTrip(ctt1, ctt2)){
                originalTrip2 = new ArrayList<>(individual.giantTour.chromosome[period][ctt2.vehicletype].subList(ctt2.startIndex, ctt2.endIndex));
                if(fitnessDifferenceOfSwap(originalTrip1, originalTrip2, ctt1Successor, ctt2) > 0){
                    performSwap(individual, ctt1Successor, ctt2);
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean twoOpt2(Individual individual, int customer, int period){
        //if customer and neighbor does not come from same route (trip), replace successor with neighbor
        CustomerToTrip ctt1 = individual.customerToTrips[period][customer];
        if (ctt1.endIndex - (ctt1.startIndex + ctt1.index) < 2){
            return false;
        }
        List<Integer> originalTrip1 = new ArrayList<>(individual.giantTour.chromosome[period][ctt1.vehicletype].subList(ctt1.startIndex, ctt1.endIndex));

        CustomerToTrip ctt1Successor = individual.customerToTrips[period][getSucceedingCustomer(individual, ctt1)];
        CustomerToTrip ctt2;
        List<Integer> originalTrip2;
        for (Customer neighbor : data.customers[customer].nearestNeighbors) {
            if (neighbor.requiredVisitPeriod[period] == 0) {
                continue;
            }
            ctt2 = individual.customerToTrips[period][neighbor.customerID];
            if (ctt2.endIndex - (ctt2.startIndex + ctt2.index) < 2 && !isSameTrip(ctt1, ctt2)){
                originalTrip2 = new ArrayList<>(individual.giantTour.chromosome[period][ctt2.vehicletype].subList(ctt2.startIndex, ctt2.endIndex));
                if(fitnessDifferenceOfSwap(originalTrip1, originalTrip2, ctt1Successor, ctt2) > 0) {
                    performSwap(individual, ctt1Successor, ctt2);
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean twoOpt3(Individual individual, int customer, int period){
        //if customer and neighbor not in same route (trip), swap customer - successor && neighbor - neighborSuccessor
        //with customer - neighborsuccessor && successor neighbor
        CustomerToTrip ctt1 = individual.customerToTrips[period][customer];
        if (ctt1.endIndex - (ctt1.startIndex + ctt1.index) < 2){
            return false;
        }
        List<Integer> originalTrip1 = new ArrayList<>(individual.giantTour.chromosome[period][ctt1.vehicletype].subList(ctt1.startIndex, ctt1.endIndex));

        CustomerToTrip ctt1Successor = individual.customerToTrips[period][getSucceedingCustomer(individual, ctt1)];
        CustomerToTrip ctt2;
        CustomerToTrip ctt2Successor;
        List<Integer> originalTrip2;
        for (Customer neighbor : data.customers[customer].nearestNeighbors) {
            if (neighbor.requiredVisitPeriod[period] == 0) {
                continue;
            }
            ctt2 = individual.customerToTrips[period][neighbor.customerID];
            if (ctt2.endIndex - (ctt2.startIndex + ctt2.index) >= 2 && !isSameTrip(ctt1, ctt2)){
                ctt2Successor = individual.customerToTrips[period][getSucceedingCustomer(individual, ctt2)];
                originalTrip2 = new ArrayList<>(individual.giantTour.chromosome[period][ctt2.vehicletype].subList(ctt2.startIndex, ctt2.endIndex));
                if(fitnessDifferenceOf3WaySwap(originalTrip1, originalTrip2, ctt1Successor, ctt2, ctt2Successor) > 0) {
                    perform3WaySwap(individual, ctt1Successor, ctt2, ctt2Successor);
                    return true;
                }
            }
        }
        return false;

    }

    private static boolean doubleInsertion(Individual individual, int customer, int period, boolean reverse){
        //inserts  customer and its successor after customers nearest neighbor if it gives improvement
        //If reverse is true, successor is placed prior to customer
        CustomerToTrip ctt1 = individual.customerToTrips[period][customer];
        if (ctt1.endIndex - (ctt1.startIndex + ctt1.index) < 2){
            return false;
        }
        int succeedingCustomerID = getSucceedingCustomer(individual, ctt1);
        CustomerToTrip succeedingCustomer = individual.customerToTrips[period][succeedingCustomerID];
        double improvementOfRemoval;
        double detoriorationOfInsertion;
        List<Integer> originalTrip1;
        List<Integer> originalTrip2;

        int c1;
        int c2;

        originalTrip1 = new LinkedList<>(individual.giantTour.chromosome[period][ctt1.vehicletype].subList(ctt1.startIndex, ctt1.endIndex));
        improvementOfRemoval = fitnessDifferenceOfRemoval2Customers(originalTrip1, period, ctt1.vehicletype, succeedingCustomer.index, ctt1.index);
        CustomerToTrip ctt2;
        for (Customer neighbor : data.customers[customer].nearestNeighbors){
            if (neighbor.requiredVisitPeriod[period] == 0 || neighbor.customerID == succeedingCustomerID){
                continue;
            }
            ctt2 = individual.customerToTrips[period][neighbor.customerID];
            originalTrip2 = new LinkedList<>(individual.giantTour.chromosome[period][ctt2.vehicletype].subList(ctt2.startIndex, ctt2.endIndex));
            c1 = reverse ? succeedingCustomer.customer : ctt1.customer;
            c2 = reverse ? ctt1.customer : succeedingCustomer.customer;
            detoriorationOfInsertion = fitnessDifferenceOfInsertion2Customers(originalTrip2, ctt2, c1, c2);
            if (improvementOfRemoval > detoriorationOfInsertion){
//                System.out.println("improvement: " + improvementOfRemoval);
//                System.out.println("detorioration: " + detoriorationOfInsertion);
//                System.out.println("current customer: " + ctt1.customer + " placed after: " + ctt2.customer);
//                System.out.println("old chromosome c1: " + individual.giantTour.chromosome[period][ctt1.vehicletype]);
//                System.out.println("old chromosome c2: " + individual.giantTour.chromosome[period][ctt2.vehicletype]);
                double total = improvementOfRemoval - detoriorationOfInsertion;

                doInsertion2Customers(individual, ctt1, ctt2, succeedingCustomer.customer, reverse);
//
//
//
//                System.out.println("new chromosome c1: " + individual.giantTour.chromosome[period][ctt1.vehicletype]);
//                System.out.println("new chromosome c2: " + individual.giantTour.chromosome[period][ctt2.vehicletype]);
                return true;
            }
        }
        return false;
    }

//  ------------------------------HELPER FUNCTIONS-------------------------------------------------------

    private static int getSucceedingCustomer(Individual individual, CustomerToTrip ctt){
        return individual.giantTour.chromosome[ctt.period][ctt.vehicletype].get(ctt.startIndex + ctt.index + 1);
    }

    private static boolean isValidSameTrip(CustomerToTrip ctt1, CustomerToTrip ctt2){
        return isSameTrip(ctt1, ctt2) && Math.abs(ctt1.index - ctt2.index) > 2;
    }

    private static boolean isSameTrip(CustomerToTrip ctt1, CustomerToTrip ctt2){
        return ctt1.vehicletype == ctt2.vehicletype && ctt1.startIndex == ctt2.startIndex;
    }

    private static double fitnessDifferenceOfRemoval(List<Integer> customerSequence, CustomerToTrip ctt){
        double initialFitness = FitnessCalculation.getTripFitness(customerSequence, ctt.vehicletype, ctt.period, orderDistribution.orderVolumeDistribution, data);
        customerSequence.remove(ctt.index);
        System.out.println(customerSequence);
        double newFitness = FitnessCalculation.getTripFitness(customerSequence, ctt.vehicletype, ctt.period, orderDistribution.orderVolumeDistribution, data);
        customerSequence.add(ctt.index, ctt.customer);
        return initialFitness - newFitness;
    }

    private static double fitnessDifferenceOfRemoval2Customers(List<Integer> customerSequence, int period, int vehicleType, int maxIndex, int minIndex){
        double initialFitness = FitnessCalculation.getTripFitness(customerSequence, vehicleType, period, orderDistribution.orderVolumeDistribution, data);
        customerSequence.remove(maxIndex);
        customerSequence.remove(minIndex);
        double newFitness = FitnessCalculation.getTripFitness(customerSequence, vehicleType, period, orderDistribution.orderVolumeDistribution, data);
        return initialFitness - newFitness;
    }

    private static double fitnessDifferenceOfInsertion(List<Integer> customerSequence, CustomerToTrip ctt, int insertedCustomer){
        //evaluates fitness difference of adding customer (insertedCustomer) after ctt.customer
        double initialFitness = FitnessCalculation.getTripFitness(customerSequence, ctt.vehicletype, ctt.period, orderDistribution.orderVolumeDistribution, data);
        customerSequence.add(ctt.index + 1, insertedCustomer);
        System.out.println(customerSequence);
        double newFitness = FitnessCalculation.getTripFitness(customerSequence, ctt.vehicletype, ctt.period, orderDistribution.orderVolumeDistribution, data);
        return newFitness - initialFitness;
    }

    private static double fitnessDifferenceOfInsertion2Customers(List<Integer> customerSequence, CustomerToTrip ctt, int insertedCustomer1, int insertedCustomer2){
        double initialFitness = FitnessCalculation.getTripFitness(customerSequence, ctt.vehicletype, ctt.period, orderDistribution.orderVolumeDistribution, data);
        customerSequence.add(ctt.index + 1, insertedCustomer2);
        customerSequence.add(ctt.index + 1, insertedCustomer1);
        double newFitness = FitnessCalculation.getTripFitness(customerSequence, ctt.vehicletype, ctt.period, orderDistribution.orderVolumeDistribution, data);
        return newFitness - initialFitness;
    }

    private static double fitnessDifferenceOfSwap(List<Integer> customerSequence1, List<Integer> customerSequence2, CustomerToTrip ctt1, CustomerToTrip ctt2){
        double oldFitness = combinedFitnessOf2Sequences(customerSequence1, customerSequence2, ctt1.vehicletype, ctt1.period, ctt2.vehicletype, ctt2.period);
        //perform swap and check new fitness
        customerSequence1.set(ctt1.index, ctt2.customer);
        customerSequence2.set(ctt2.index, ctt1.customer);
        double newFitness = combinedFitnessOf2Sequences(customerSequence1, customerSequence2, ctt1.vehicletype, ctt1.period, ctt2.vehicletype, ctt2.period);
        //swap back to original
        customerSequence1.set(ctt1.index, ctt1.customer);
        customerSequence2.set(ctt2.index, ctt2.customer);
        return newFitness - oldFitness;
    }

    private static double fitnessDifferenceOfDoubleSwap(List<Integer> customerSequence1, List<Integer> customerSequence2, CustomerToTrip ctt1, CustomerToTrip ctt1Successor, CustomerToTrip ctt2, CustomerToTrip ctt2Successor){
        double oldFitness = combinedFitnessOf2Sequences(customerSequence1, customerSequence2, ctt1.vehicletype, ctt1.period, ctt2.vehicletype, ctt2.period);
        customerSequence1.set(ctt1.index, ctt2.customer);
        customerSequence2.set(ctt2.index, ctt1.customer);
        customerSequence1.set(ctt1Successor.index, ctt2Successor.customer);
        customerSequence2.set(ctt2Successor.index, ctt1Successor.customer);
        double newFitness = combinedFitnessOf2Sequences(customerSequence1, customerSequence2, ctt1.vehicletype, ctt1.period, ctt2.vehicletype, ctt2.period);
        //swap back to original
        customerSequence1.set(ctt1.index, ctt1.customer);
        customerSequence2.set(ctt2.index, ctt2.customer);
        customerSequence1.set(ctt1Successor.index, ctt1Successor.customer);
        customerSequence2.set(ctt2Successor.index, ctt2Successor.customer);
        return newFitness - oldFitness;

    }

    private static double fitnessDifferenceOf3WaySwap(List<Integer> customerSequence1, List<Integer> customerSequence2, CustomerToTrip ctt1, CustomerToTrip ctt2, CustomerToTrip ctt3){
        //swaps ctt1 into ctt2, ctt2 into ctt3, and ctt3 into ctt1
        double oldFitness = combinedFitnessOf2Sequences(customerSequence1, customerSequence2, ctt1.vehicletype, ctt1.period, ctt2.vehicletype, ctt2.period);
        customerSequence2.set(ctt2.index, ctt1.customer);
        customerSequence2.set(ctt3.index, ctt2.customer);
        customerSequence1.set(ctt1.index, ctt3.customer);
        double newFitness = combinedFitnessOf2Sequences(customerSequence1, customerSequence2, ctt1.vehicletype, ctt1.period, ctt2.vehicletype, ctt2.period);
        //set back to original
        customerSequence2.set(ctt2.index, ctt2.customer);
        customerSequence2.set(ctt3.index, ctt3.customer);
        customerSequence1.set(ctt1.index, ctt1.customer);
        return newFitness - oldFitness;

    }

    private static double fitnessDifferenceOfSwapAndInsertion(List<Integer> customerSequence1, List<Integer> customerSequence2, CustomerToTrip ctt1, CustomerToTrip ctt2, CustomerToTrip ctt3){
        double oldFitness = combinedFitnessOf2Sequences(customerSequence1, customerSequence2, ctt1.vehicletype, ctt1.period, ctt2.vehicletype, ctt2.period);
        customerSequence1.set(ctt1.index, ctt2.customer);
        customerSequence2.set(ctt2.index, ctt1.customer);
        customerSequence1.remove(ctt3.index);
        customerSequence2.add(ctt2.index + 1, ctt3.customer);
        double newFitness = combinedFitnessOf2Sequences(customerSequence1, customerSequence2, ctt1.vehicletype, ctt1.period, ctt2.vehicletype, ctt2.period);
        customerSequence2.remove(ctt2.index + 1);
        customerSequence1.add(ctt3.index, ctt3.customer);
        customerSequence2.set(ctt2.index, ctt2.customer);
        customerSequence1.set(ctt1.index, ctt2.customer);
        return newFitness - oldFitness;

    }

    private static double combinedFitnessOf2Sequences(List<Integer> customerSequence1, List<Integer> customerSequence2, int vt1, int period1, int vt2, int period2){
        return FitnessCalculation.getTripFitness(customerSequence1, vt1, period1, orderDistribution.orderVolumeDistribution, data)
                + FitnessCalculation.getTripFitness(customerSequence2, vt2, period2, orderDistribution.orderVolumeDistribution, data);
    }

    private static void doRemovalInsertion(Individual individual, CustomerToTrip removeCustomer, CustomerToTrip insertCustomer, boolean update){
        int adjustIndex = removeCustomer.vehicletype == insertCustomer.vehicletype && removeCustomer.startIndex + removeCustomer.index < insertCustomer.startIndex + insertCustomer.index ? 0 : 1;
        individual.giantTour.chromosome[removeCustomer.period][removeCustomer.vehicletype].remove(removeCustomer.startIndex + removeCustomer.index);
        individual.giantTour.chromosome[insertCustomer.period][insertCustomer.vehicletype].add(insertCustomer.startIndex + insertCustomer.index + adjustIndex, removeCustomer.customer);
        if(update){
            updateAdsplit(individual, removeCustomer);
            updateAdsplit(individual, insertCustomer);
        }
    }

    private static void doRemovalInsertion(Individual individual, CustomerToTrip removeCustomer, CustomerToTrip insertCustomer){
        doRemovalInsertion(individual, removeCustomer, insertCustomer, true);
    }

    private static void doInsertion2Customers(Individual individual, CustomerToTrip removeCustomer, CustomerToTrip insertCustomer, int succeedingCustomer, boolean reverse){
        int adjustIndex = removeCustomer.vehicletype == insertCustomer.vehicletype && removeCustomer.startIndex + removeCustomer.index < insertCustomer.startIndex + insertCustomer.index ? -1 : 1;
        individual.giantTour.chromosome[removeCustomer.period][removeCustomer.vehicletype].remove(removeCustomer.startIndex + removeCustomer.index);
        individual.giantTour.chromosome[removeCustomer.period][removeCustomer.vehicletype].remove(removeCustomer.startIndex + removeCustomer.index);
        if (reverse){
            individual.giantTour.chromosome[insertCustomer.period][insertCustomer.vehicletype].add(insertCustomer.startIndex + insertCustomer.index + adjustIndex, removeCustomer.customer);
            individual.giantTour.chromosome[insertCustomer.period][insertCustomer.vehicletype].add(insertCustomer.startIndex + insertCustomer.index + adjustIndex, succeedingCustomer);
        }
        else{
            individual.giantTour.chromosome[insertCustomer.period][insertCustomer.vehicletype].add(insertCustomer.startIndex + insertCustomer.index + adjustIndex, succeedingCustomer);
            individual.giantTour.chromosome[insertCustomer.period][insertCustomer.vehicletype].add(insertCustomer.startIndex + insertCustomer.index + adjustIndex, removeCustomer.customer);
        }
        updateAdsplit(individual, removeCustomer);
        updateAdsplit(individual, insertCustomer);
    }

    private static void perform3WaySwap(Individual individual, CustomerToTrip ctt1, CustomerToTrip ctt2, CustomerToTrip ctt3){
        individual.giantTour.chromosome[ctt2.period][ctt2.vehicletype].set(ctt2.startIndex + ctt2.index, ctt1.customer);
        individual.giantTour.chromosome[ctt3.period][ctt3.vehicletype].set(ctt3.startIndex + ctt3.index, ctt2.customer);
        individual.giantTour.chromosome[ctt1.period][ctt1.vehicletype].set(ctt1.startIndex + ctt1.index, ctt3.customer);
        updateAdsplit(individual, ctt1);
        updateAdsplit(individual, ctt2);
    }

    private static void performSwap(Individual individual, CustomerToTrip ctt1, CustomerToTrip ctt2, boolean update){
        individual.giantTour.chromosome[ctt1.period][ctt1.vehicletype].set(ctt1.startIndex + ctt1.index, ctt2.customer);
        individual.giantTour.chromosome[ctt2.period][ctt2.vehicletype].set(ctt2.startIndex + ctt2.index, ctt1.customer);
        if (update){
            updateAdsplit(individual, ctt1);
            updateAdsplit(individual, ctt2);
        }
    }

    private static void performSwap(Individual individual, CustomerToTrip ctt1, CustomerToTrip ctt2){
        performSwap(individual, ctt1, ctt2, true);
    }

    private static void updateAdsplit(Individual individual, CustomerToTrip customerToTrip){
        AdSplit.adSplitSingular(individual, customerToTrip.period, customerToTrip.vehicletype);
        individual.makeCustomerToTripMapSingular(customerToTrip.period, customerToTrip.vehicletype);
    }










    public static void main(String[] args){
        Data data = DataReader.loadData();

        for (int i = 0 ; i < 1 ; i++){
            System.out.println(i);

            Individual individual = new Individual(data);
            OrderDistribution od = new OrderDistribution(data);
            od.makeInitialDistribution();
            individual.initializeIndividual(od);



            AdSplit.adSplitPlural(individual);
            individual.makeCustomerToTripMap();

            System.out.println(individual.giantTourSplit.chromosome);
            improveRoutes(individual, individual.orderDistribution);
        }
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
