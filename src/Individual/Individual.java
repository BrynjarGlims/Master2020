package Individual;
import DataFiles.*;
import ProductAllocation.OrderDistribution;

import javax.lang.model.type.ArrayType;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class Individual {



    //chromosome data
    public GiantTour giantTour;  //period, vehicleType
    public VehicleAssigment vehicleAssigment;
    public GiantTourSplit giantTourSplit;

    public Data data;

    public ArrayList<ArrayList<Integer>>[][] matrixOfTrips;
    public ArrayList<Double>[][] matrixOfTripCosts;



    public OrderDistribution orderDistribution;
    public ArrayList<LabelPool> labelPools;


    public int[][] arcCost;  // (i,j) i = from, j = to

    public LabelPool[][] lastLabelPool;
    public Label[][] bestLabel;

    private ArrayList[][] listOfTrips;
    private double[][] arcCostMatrix;


    public Individual(Data data, OrderDistribution orderDistribution) {
        this.data = data;
        this.orderDistribution = orderDistribution;

        //set chromosome
        this.vehicleAssigment = new VehicleAssigment(data);
        this.giantTourSplit = new GiantTourSplit(data);
        this.giantTour = new GiantTour(data);


        this.lastLabelPool = new LabelPool[data.numberOfPeriods][data.numberOfVehicleTypes];
        this.bestLabel = new Label[data.numberOfPeriods][data.numberOfVehicleTypes];
        this.matrixOfTrips = new ArrayList[data.numberOfPeriods][data.numberOfVehicleTypes];
        this.matrixOfTripCosts = new ArrayList[data.numberOfPeriods][data.numberOfVehicleTypes];


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


    //------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    //SHORTEST PATH METHODS:

    public void createTrips(int p, int vt) {
        //SHORTEST PATH
        ArrayList<Integer> customerSequence = this.giantTour.chromosome[p][vt];

        //insert depot to be in the 0th position
        customerSequence.add(0, data.customers.length);
        double[] costLabel = new double[customerSequence.size()];
        int[] predecessorLabel = new int[customerSequence.size()];


        for (int i = 0; i < customerSequence.size(); i++) {
            costLabel[i] = 100000;
            predecessorLabel[i] = 0;
        }
        costLabel[0] = 0;
        for (int i = 0; i < customerSequence.size(); i++) {
            double loadSum = 0;
            double distanceCost = 0;
            int j = i+1;
            while (j < customerSequence.size()) {
                loadSum += this.orderDistribution.orderDistribution[p][customerSequence.get(j)];
                if (j == (i + 1)) {
                    distanceCost = data.distanceMatrix[customerSequence.get(j)][data.customers.length];
                } else {
                    distanceCost = data.distanceMatrix[customerSequence.get(j - 1)][customerSequence.get(j)] + data.distanceMatrix[customerSequence.get(j)][data.customers.length];
                }
                if (costLabel[i] + distanceCost < costLabel[j] && loadSum <= data.vehicleTypes[vt].capacity) {
                    costLabel[j] = costLabel[i] + distanceCost;
                    predecessorLabel[j] = i;

                }
                j += 1;
            }
        }

        extractVrpSolution(customerSequence, predecessorLabel, p, vt);
    }
    public void extractVrpSolution(ArrayList<Integer> customerSequence, int[] predecessorLabel, int p, int vt) {
        //extract VRP solution by backtracking the shortest path label
        ArrayList<ArrayList<Integer>> listOfTrips = new ArrayList<ArrayList<Integer>>();
        ArrayList<Integer> tempListOfTrips = new ArrayList<Integer>();

        //TODO: last element in giantTour often neglected. why? debug.
        for (int k = 1; k < customerSequence.size(); k++) {
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

        //Calculate trip costs
        ArrayList<Double> listOfTripCosts = new ArrayList<Double>(listOfTrips.size());
        for (List<Integer> list : listOfTrips) {
            double tripCost = 0;
            if (list.size() == 1) {
                tripCost += data.distanceMatrix[data.customers.length][list.get(0)] + data.distanceMatrix[list.get(list.size() - 1)][data.customers.length];
            }
            if (list.size() > 1) {
                tripCost += data.distanceMatrix[data.customers.length][list.get(0)] + data.distanceMatrix[list.get(list.size() - 1)][data.customers.length];
                for (int i = 1; i < list.size() - 1; i++) {
                    tripCost += data.distanceMatrix[list.get(i)][list.get(i + 1)];
                }
            }
            listOfTripCosts.add(tripCost);
        }
        this.matrixOfTrips[p][vt] = listOfTrips;
        this.matrixOfTripCosts[p][vt] = listOfTripCosts;
    }
    //------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------





    public void labelingAlgorithm(int p, int vt, ArrayList<ArrayList<Integer>> listOfTrips, ArrayList<Double> arcCost) {

        // take this as input,remove afterwards
        this.labelPools = new ArrayList<LabelPool>();  //reset the previous label pool


        for (int tripNumber = 0; tripNumber < listOfTrips.size(); tripNumber++) {
            if (tripNumber == 0) {
                LabelPool newLabelPool = new LabelPool(data, listOfTrips, tripNumber, orderDistribution.orderDistribution);
                newLabelPool.generateFirstLabel(data.numberOfVehiclesInVehicleType[vt],
                        arcCost.get(tripNumber), p, vt);
                labelPools.add(newLabelPool);
            } else {
                LabelPool newLabelPool = new LabelPool(data, listOfTrips, tripNumber, orderDistribution.orderDistribution);
                newLabelPool.generateLabels(labelPools.get(labelPools.size() - 1), arcCost.get(tripNumber));
                newLabelPool.removeDominated();
                labelPools.add(newLabelPool);
            }
        }
        
        if (labelPools.size() > 0){
            this.lastLabelPool[p][vt] = labelPools.get(labelPools.size()-1);
            this.bestLabel[p][vt] = lastLabelPool[p][vt].findBestLabel();
        }
        else{
            System.out.println("No best label found");
            // TODO: 17.02.2020 Problem with giant tour being empty 
        }

    }


    //solves for each period
    public void adSplit() {
        for (int p = 0; p < data.numberOfPeriods; p++) {
            for (int vt = 0; vt < this.data.numberOfVehicleTypes; vt++) {
                if (giantTour.chromosome[p][vt].size() == 0){
                    continue;
                }
                //Shortest path algorithm
                createTrips(p, vt);

                //Labeling algorithm
                labelingAlgorithm(p, vt, matrixOfTrips[p][vt], matrixOfTripCosts[p][vt]);   // Sets bestLabel.

                //Set vehicleAssignment
                vehicleAssigment.setChromosome(bestLabel[p][vt].getVehicleAssignmentList(), p, vt);
                //Set giantTourSplit
                giantTourSplit.setChromosome(createSplitChromosome(matrixOfTrips[p][vt]), p, vt);
            }
        }
    }

    public ArrayList<Integer> createSplitChromosome(ArrayList<ArrayList<Integer>> customerSequence){
        ArrayList<Integer> splits = new ArrayList<>();
        int split = 0;

        for (ArrayList<Integer> tripList: customerSequence){
            split += tripList.size();
            splits.add(split);
        }
        return splits;

    }


    public static void main(String[] args){
        Data data = DataReader.loadData();
        OrderDistribution od = new OrderDistribution(data);
        od.makeDistribution();
        Individual individual = new Individual(data, od);
        individual.adSplit();

        //todo: implement
        individual.giantTour.toString();
        individual.giantTourSplit.toString();
        individual.vehicleAssigment.toString();


    }


}








