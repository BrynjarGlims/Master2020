package Individual;
import DataFiles.*;
import Population.Population;
import ProductAllocation.OrderDistribution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


public class Individual {
    //chromosome data
    public GiantTour giantTour;  //period, vehicleType
    public VehicleAssigment vehicleAssigment;
    public GiantTourSplit giantTourSplit;
    public OrderDistribution orderDistribution;

    public Data data;
    public boolean validCapacity;

    public double infeasibilityOvertimeValue;
    public double infeasibilityTimeWarpValue;
    public double infeasibilityOverCapacityValue;


    //// TODO: 18.02.2020 TO be removed
    public ArrayList<ArrayList<Integer>>[][] matrixOfTrips;
    public ArrayList<Double>[][] matrixOfTripCosts;
    public Label[][] bestLabels;
    public Population Population;


    public Individual(Data data, OrderDistribution orderDistribution) {
        this.data = data;
        this.orderDistribution = orderDistribution;

        this.infeasibilityOverCapacityValue = 0;
        this.infeasibilityOvertimeValue = 0;
        this.infeasibilityTimeWarpValue = 0;

        //set chromosome
        this.vehicleAssigment = new VehicleAssigment(data);
        this.giantTourSplit = new GiantTourSplit(data);
        this.giantTour = new GiantTour(data);

        this.bestLabels = new Label[data.numberOfPeriods][data.numberOfVehicleTypes];
        this.matrixOfTrips = new ArrayList[data.numberOfPeriods][data.numberOfVehicleTypes];
        this.matrixOfTripCosts = new ArrayList[data.numberOfPeriods][data.numberOfVehicleTypes];

        this.adSplit(); //perform adSplit

    }

    public boolean isFeasible() {
        return (infeasibilityOverCapacityValue == 0 && infeasibilityOvertimeValue == 0
                && infeasibilityTimeWarpValue == 0);

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

    public double[] getInitialCostLabel(int size){
        double[] costLabel =  new double[size];
        Arrays.fill(costLabel, 100000);
        costLabel[0] = 0;
        return costLabel;
    }

    public int[] getInitialPredecessorLabel(int size){
        int[] predecessorLabel =  new int[size];
        Arrays.fill(predecessorLabel, 0);
        return predecessorLabel;
    }


    public void createTrips(int p, int vt) {
        //SHORTEST PATH
        ArrayList<Integer> customerSequence = (ArrayList<Integer>) giantTour.chromosome[p][vt].clone();  //// TODO: 18.02.2020 Brynar: is this superfast or only fast?
        customerSequence.add(0, data.customers.length);

        //insert depot to be in the 0th position

        double[] costLabel = this.getInitialCostLabel(customerSequence.size());
        int[] predecessorLabel = new int[customerSequence.size()];
        double loadSum;
        double distanceCost;


        for (int i = 0; i < customerSequence.size(); i++) {
            loadSum = 0;
            for( int j = i+1; j < customerSequence.size(); j++ ) {   //todo: make this a for element in list function.
                loadSum += this.orderDistribution.orderDistribution[p][customerSequence.get(j)];
                if (j == (i + 1)) {
                    distanceCost = data.distanceMatrix[customerSequence.get(j)][data.customers.length];
                } else {
                    distanceCost = data.distanceMatrix[customerSequence.get(j - 1)][customerSequence.get(j)]
                            + data.distanceMatrix[customerSequence.get(j)][data.customers.length];
                }
                if (costLabel[i] + distanceCost < costLabel[j] && loadSum <= data.vehicleTypes[vt].capacity) {
                    costLabel[j] = costLabel[i] + distanceCost;
                    predecessorLabel[j] = i;
                }
            }
        }
        extractVrpSolution(customerSequence, predecessorLabel, p, vt);
    }

    public ArrayList<ArrayList<Integer>> setListOfTrips(ArrayList<Integer> customerSequence, int[] predecessorLabel){

        ArrayList<ArrayList<Integer>> listOfTrips = new ArrayList<ArrayList<Integer>>();
        ArrayList<Integer> tempListOfTrips = new ArrayList<Integer>();

        if (predecessorLabel.length == 2) {
            tempListOfTrips.add(customerSequence.get(1));
            listOfTrips.add(tempListOfTrips);
        }
        else if (predecessorLabel.length > 2) {
            tempListOfTrips.add(customerSequence.get(1));
            for (int k = 2; k < customerSequence.size(); k++) {
                if (predecessorLabel[k] == 0) {
                    listOfTrips.add(tempListOfTrips);
                    tempListOfTrips = new ArrayList<>();
                    tempListOfTrips.add(customerSequence.get(k));
                }
                else if (predecessorLabel[k] == k-1) {
                    tempListOfTrips.add(customerSequence.get(k));
                }
                if (k == (customerSequence.size()-1)) {
                    listOfTrips.add(tempListOfTrips);
                }
            }
        }
        return listOfTrips;
    }


    public void extractVrpSolution(ArrayList<Integer> customerSequence, int[] predecessorLabel, int p, int vt) {
        //extract VRP solution by backtracking the shortest path label

        ArrayList<ArrayList<Integer>> listOfTrips = setListOfTrips( customerSequence, predecessorLabel);
        double tripCost;
        //Calculate trip costs
        ArrayList<Double> listOfTripCosts = new ArrayList<Double>(listOfTrips.size());
        for (List<Integer> list : listOfTrips) {
            tripCost = 0;
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

    public void labelingAlgorithm(int p, int vt, ArrayList<ArrayList<Integer>> listOfTrips, ArrayList<Double> arcCost) {

        int tripNumber = 0;
        LabelPool currentLabelPool = new LabelPool(data, listOfTrips, tripNumber, orderDistribution.orderDistribution);
        LabelPool nextLabelPool;

        while(tripNumber < listOfTrips.size()) {
            if (tripNumber == 0) {
                currentLabelPool.generateFirstLabel(data.numberOfVehiclesInVehicleType[vt],
                        arcCost.get(tripNumber), p, vt);
                tripNumber++;
            } else {
                nextLabelPool = new LabelPool(data, listOfTrips, tripNumber, orderDistribution.orderDistribution);
                nextLabelPool.generateLabels(currentLabelPool, arcCost.get(tripNumber));
                nextLabelPool.removeDominated();
                currentLabelPool = nextLabelPool;
                tripNumber++;
            }
        }
        if (currentLabelPool.labels.size() > 0){
            this.bestLabels[p][vt] = currentLabelPool.findBestLabel();
        }

    }

    //solves for each period
    public void adSplit() {
        for (int p = 0; p < data.numberOfPeriods; p++) {
            for (int vt = 0; vt < this.data.numberOfVehicleTypes; vt++) {
                
                if (giantTour.chromosome[p][vt].size()==0) {
                    continue;
                }
                //Shortest path algorithm
                createTrips(p, vt);
                //Labeling algorithm
                labelingAlgorithm(p, vt, matrixOfTrips[p][vt], matrixOfTripCosts[p][vt]);   // Sets bestLabel.
                //// TODO: 18.02.2020 Implement an improved split procedure that reorders customers
                //Set vehicleAssignment
                vehicleAssigment.setChromosome(this.bestLabels[p][vt].getVehicleAssignmentChromosome(), p , vt);
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

    public int getRankOfIndividual() {
        int rank = 0; //TODO: implement rank calculations
        return rank;
    }

    public double getIndividualBiasedFitnessScore() {
        double fitness = 0.0; //TODO: implement fitness calculations
        //calculate biased fitness element
        int nbIndividuals = 0;
        if (this.isFeasible()) {
            nbIndividuals = Population.getSizeOfFeasiblePopulation();
        }
        else if (!this.isFeasible()) {
            nbIndividuals = Population.getSizeOfInfeasiblePopulation();
        }
        double biasedfitness = (1 - (Parameters.numberOfEliteIndividuals/nbIndividuals)*getRankOfIndividual());
        double fitnessScore = fitness + biasedfitness;

        /*
        //Use the following code if we do not operate two subpopulations: infeasible and feasible individuals
        double P = 0;
        if (isFeasible()) {
            P = 1;
        } else {

            if (this.infeasibilityOvertimeValue > 0 && this.infeasibilityOverCapacityValue >0 && this.infeasibilityTimeWarpValue >0) {
                P = 3;
            }
            else if ((this.infeasibilityOverCapacityValue > 0 && this.infeasibilityTimeWarpValue > 0) || (this.infeasibilityOverCapacityValue > 0 && this.infeasibilityOvertimeValue > 0) || (this.infeasibilityTimeWarpValue > 0 && this.infeasibilityOvertimeValue > 0) ) {
                P = 2;
            }
            else if (this.infeasibilityOvertimeValue > 0 || this.infeasibilityTimeWarpValue > 0 || this.infeasibilityOverCapacityValue > 0) {
                P = 1;
            }
        fitnessScore *= P;
        }
         */
        return fitnessScore;
    }


    public static void main(String[] args){
        Data data = DataReader.loadData();
        OrderDistribution od = new OrderDistribution(data);
        od.makeDistribution();
        Individual individual = new Individual(data, od);
        individual.adSplit();
        System.out.println("hei");

        //todo: implement
        individual.giantTour.toString();
        individual.giantTourSplit.toString();
        individual.vehicleAssigment.toString();


    }


}








