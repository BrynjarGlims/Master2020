package Individual;
import DataFiles.*;
import ProductAllocation.OrderDistribution;
import org.w3c.dom.ls.LSOutput;
import scala.xml.PrettyPrinter;

import java.awt.color.ICC_Profile;
import java.util.ArrayList;
import java.util.List;

public class Individual {

    //TODO: The giantTour class will be a 2D array, s.t. one chromosome represents giant tours for all (period, vehicleType)
    public GiantTour[][] giantTours;  //period, vehicleType
    public VehicleType vehicleType;
    public double costOfIndividual;
    public OrderDistribution orderDistribution;

    public Data data;
    public int[][] arcCost;  // (i,j) i = from, j = to

    public Individual(Data data, OrderDistribution orderDistribution) {
        this.data = data;
        this.orderDistribution = orderDistribution;
        this.giantTours = new GiantTour[data.numberOfPeriods][data.numberOfVehicleTypes];
        for (int p = 0; p < data.numberOfPeriods; p++) {
            for (int vt = 0; vt < data.numberOfVehicleTypes; vt++) {
                giantTours[p][vt] = new GiantTour();
            }
        }
    }

    public boolean isFeasible() {
        //NOTE: AdSplit must be called in advance of this method
        if (!hasValidTimeWindows()) {
            return false;
        } else if (!hasValidVehicleCapacity()) {
            return false;

        }
        return true;
    }

    public boolean hasValidTimeWindows() {
        //Todo: needs to be implemented
        return true;
    }

    public boolean hasValidVehicleCapacity() {
        //Todo: needs to be implemented
        return true;
    }

    public double evaluateIndividual() {
        //TODO: needs to be implemented
        return 0.0;
    }

    public void createTripsNew(int p, int vt) {
        //SHORTEST PATH
        ArrayList<Integer> customerSequence = this.giantTours[p - 1][vt - 1].chromosome;

        //insert depot to be in the 0th position
        customerSequence.add(0, data.customers.length);
        double[] costLabel = new double[customerSequence.size()];
        int[] predecessorLabel = new int[customerSequence.size()];


        for (int i = 0; i < customerSequence.size(); i++) {
            costLabel[i] = 100000;
            predecessorLabel[i] = data.customers.length;
        }
        costLabel[0] = 0;

        for (int i = 0; i < customerSequence.size(); i++) {
            double loadSum = 0;
            double distanceCost = 0;
            for (int j = i + 1; j < customerSequence.size(); j++) {
                loadSum += this.orderDistribution.orderDistribution[p - 1][customerSequence.get(j)];
                if (j == i + 1) {
                    distanceCost = data.distanceMatrix[customerSequence.get(j)][data.customers.length];
                } else {
                    distanceCost = data.distanceMatrix[customerSequence.get(j - 1)][customerSequence.get(j)] + data.distanceMatrix[customerSequence.get(j)][data.customers.length];
                }
                if (costLabel[i] + distanceCost < costLabel[j] && loadSum <= data.vehicleTypes[vt - 1].capacity) {
                    costLabel[j] = costLabel[i] + distanceCost;
                    predecessorLabel[j] = i;
                    System.out.println("Predecessor updated: " + i);
                }

            }

        }
        extractVrpSolution(customerSequence, predecessorLabel);
    }

    public ArrayList<ArrayList<Integer>> extractVrpSolution(ArrayList<Integer> customerSequence, int[] predecessorLabel) {
        //extract VRP solution by backtracking the shortest path label
        ArrayList<ArrayList<Integer>> listOfTrips = new ArrayList<ArrayList<Integer>>();
        ArrayList<Integer> tempListOfTrips = new ArrayList<Integer>();

        for (int k = 1; k < customerSequence.size(); k++) {
            System.out.println(predecessorLabel[k]);
            if (predecessorLabel[k] != 0) {
                tempListOfTrips.add(customerSequence.get(k));
            } else if (predecessorLabel[k] == 0) {
                tempListOfTrips.add(customerSequence.get(k));
                //System.out.println("tempList added to listOfTrips: " + tempListOfTrips);
                listOfTrips.add(tempListOfTrips);
                tempListOfTrips = new ArrayList<>();
                //System.out.println("cleared, tempList = "+ tempListOfTrips);
            }
        }

        ArrayList<Double> listOfTripCosts = new ArrayList<Double>(listOfTrips.size());

        for (List<Integer> list: listOfTrips) {
            double tripCost = 0;
            tripCost += data.distanceMatrix[data.customers.length][list.get(0)] + data.distanceMatrix[list.get(list.size()-1)][data.customers.length];
            if (list.size() > 1) {
                for (int i = 1; i < list.size()-1; i++) {
                    tripCost += data.distanceMatrix[customerSequence.get(list.get(i))][customerSequence.get(list.get(i+1))];
                }
            }
            listOfTripCosts.add(tripCost);
        }
        System.out.println(listOfTrips);
        System.out.println(listOfTripCosts);

        return listOfTrips;
    }

    public static void main(String[] args) {
        Data data = DataReader.loadData();
        OrderDistribution od = new OrderDistribution(data);
        od.makeDistribution();
        Individual individual = new Individual(data, od);
        individual.createTripsNew(data.numberOfPeriods - 1, data.numberOfVehicleTypes - 1);
            // Print the orderDistribution
        /*
        for (int i = 0; i < od.orderDistribution.length; i++) {
            for (int j = 0; j < od.orderDistribution[0].length; j++) {
                System.out.println("Period: " + i + ", customer: " + j + ", Order:" + od.orderDistribution[i][j]);
            }
        }

        System.out.println("Length of customer list: " + data.customers.length);
        System.out.println("GiantTour for a period, vehicleType: " + individual.giantTours[0][1].chromosome);
        System.out.println("Periods: " + data.numberOfPeriods);
        System.out.println("Vehicle types: " + data.numberOfVehicleTypes);

         */
    }

}








