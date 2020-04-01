package Individual;
import DataFiles.*;
import Genetic.FitnessCalculation;
import Population.Population;
import ProductAllocation.OrderDistribution;

import java.util.ArrayList;
import java.util.HashMap;


public class Individual implements Comparable<Individual> {
    //chromosomes
    public GiantTour giantTour;  //period, vehicleType
    public OrderDistribution orderDistribution;
    public Population population;
    public HashMap< Integer, HashMap<Integer, Trip>> tripMap; //period, customer => trip
    public ArrayList<Trip>[][] tripList; //period, vehicleType
    public ArrayList<Journey>[][] journeyList; //period, vehicleType
    public Data data;

    public double adSplitInfeasibilityOvertimeDrivngValue;
    public double adSplitInfeasibilityTimeWarpValue;
    public double adSplitInfeasibilityOverCapacityValue;
    public Label[][] bestLabels;

    //fitness values:
    public double travelCost;
    public double infeasibilityCost; //both time warp and overload
    public double vehicleUsageCost;

    private double fitness = Double.MAX_VALUE;
    private double diversity = -1;
    private double biasedFitness;

    private double diversityRank;
    private double fitnessRank;
    public boolean isSurvivor;


    public Individual(Data data) {
        this.data = data;
        this.giantTour = new GiantTour(data);
        this.bestLabels = new Label[data.numberOfPeriods][data.numberOfVehicleTypes];
        this.initializeTripMap();
        this.initializeTripList();
        this.initializeJourneyList();
    }


    public Individual(Data data, Population population) {
        this(data);
        this.population = population;
    }

    public void initializeTripList(){
        this.tripList = new ArrayList[data.numberOfPeriods][data.numberOfVehicleTypes];
        for (int p = 0 ; p < data.numberOfPeriods; p++){
            for (int vt = 0; vt < data.numberOfVehicleTypes; vt++){
                this.tripList[p][vt] = new ArrayList<Trip>();
            }
        }
    }

    public void initializeTripMap(){
        this.tripMap = new HashMap<Integer, HashMap<Integer, Trip>>();
        for (int p = 0; p < data.numberOfPeriods; p++){
            this.tripMap.put(p, new HashMap<Integer, Trip>());
        }
    }

    public void setFitnessRank(int rank){
        this.fitnessRank = rank;
    }

    public void setDiversityRank(int rank){
        this.diversityRank = rank;
    }

    public void initializeJourneyList(){
        this.journeyList = new ArrayList[data.numberOfPeriods][data.numberOfVehicleTypes];
        for (int p = 0 ; p < data.numberOfPeriods; p++){
            for (int vt = 0; vt < data.numberOfVehicleTypes; vt++){
                this.journeyList[p][vt] = new ArrayList<Journey>();
            }
        }
    }

    public double getDiversity(){
        if (diversity == -1){
            System.out.println("Diversity not calculated, returns 0");
            return 0;
        }
        return diversity;
    }

    public void initializeIndividual(OrderDistribution od) {
        //set chromosome
        this.orderDistribution = od;
        giantTour.initializeGiantTour();
        this.adSplitInfeasibilityOverCapacityValue = 0;
        this.adSplitInfeasibilityOvertimeDrivngValue = 0;
        this.adSplitInfeasibilityTimeWarpValue = 0;
    }

    public void setTripMap( HashMap< Integer, HashMap<Integer, Trip>> tripMap ){
        this.tripMap = tripMap;

    }

    public void setTripList( ArrayList<Trip>[][] tripList){
        this.tripList = tripList;
    }

    public void setGiantTour(GiantTour gt){
        this.giantTour = gt;
    }

    public void setOptimalOrderDistribution(OrderDistribution orderDistribution){
        setOptimalOrderDistribution(orderDistribution, true);
    }

    public void     setOptimalOrderDistribution(OrderDistribution orderDistribution, boolean doAdSplit) {
        this.orderDistribution = orderDistribution;
        if (doAdSplit){
            AdSplit.adSplitPlural(this);
        }
        this.updateFitness();
    }

    public void setOrderDistribution(OrderDistribution od){
        this.orderDistribution = od;
    }

    public void testNewOrderDistribution(OrderDistribution orderDistribution){
        if (orderDistribution.equals(this.orderDistribution)){
            return;
        }
        boolean isFeasible = this.isFeasible();
        double currentFitness = this.getFitness(false);
        OrderDistribution currentOrderDistribution = this.orderDistribution;
        this.setOptimalOrderDistribution(orderDistribution);
        boolean madeInfeasible = (isFeasible == true && this.isFeasible() == false);
        if ((this.getFitness(false) > currentFitness) || madeInfeasible){  // // TODO: 05.03.2020 Make more efficient
            this.setOptimalOrderDistribution(currentOrderDistribution);
        }
    }

    public void setGiantTourFromTrips(){
        //updates giantTour chromosome from trips changed in education
        GiantTour gt = new GiantTour(data);
        for (int period = 0 ; period < data.numberOfPeriods ; period++){
            for (int vehicleType = 0 ; vehicleType < data.numberOfVehicleTypes ; vehicleType++){
                setGiantTourFromTripsPerPeriodVehicleType(period, vehicleType, gt);
            }
        }
        this.giantTour = gt;
    }

    public void setGiantTourFromTripsPerPeriodVehicleType(int p, int vt, GiantTour gt){
        ArrayList<Integer> giantTourEntry = new ArrayList<>();
        gt.chromosome[p][vt] = giantTourEntry;
        for (Trip trip : tripList[p][vt]){
            giantTourEntry.addAll(trip.customers);
        }
    }

    public boolean isFeasible() {
        return (infeasibilityCost == 0);
    }

    public OrderDistribution getOrderDistribution() {
        return orderDistribution;
    }

    public int getRankOfIndividual() {
        int rank = 0; //TODO: implement rank calculations
        return rank;
    }

    public double getFitness(boolean update){
        return getFitness(update, 1);
    }

    public double getFitness(boolean update, double penaltyMultiplier) {
        //penalty multiplier is used during repair, in order to scale down
        if (update || this.fitness == Double.MAX_VALUE) {
            updateFitness(penaltyMultiplier);
            return fitness;
        } else {
            return fitness;
        }
    }

    public void updateFitness(){
        updateFitness(1);
    }

    public void updateFitness(double penaltyMultiplier) {
        //Calculate objective costs
        double[] fitnesses = FitnessCalculation.getIndividualFitness(this, penaltyMultiplier);
        this.travelCost = fitnesses[0];
        this.infeasibilityCost = fitnesses[1];
        this.vehicleUsageCost = fitnesses[2];
        this.fitness = this.travelCost + this.vehicleUsageCost + this.infeasibilityCost + this.orderDistribution.getFitness();
    }


    public double getBiasedFitness() {
        return biasedFitness;
    }

    public void calculateBiasedFitness(){
        double diversityScaling;
        if (this.isFeasible()){
            diversityScaling = 1.0 - ((double) Parameters.minimumSubIndividualPopulationSize/ (double) this.population.feasiblePopulation.size());
        }
        else{
            diversityScaling = 1.0 - ((double) Parameters.minimumSubIndividualPopulationSize/ (double) this.population.infeasiblePopulation.size());
        }
        biasedFitness = (double) fitnessRank + (diversityScaling * (double) diversityRank);
    }


    public void setDiversity(double diversity){
        this.diversity = diversity;
    }

    public String toString(){
        String out = "";
        for (int p = 0 ; p < data.numberOfPeriods ; p++){
            out += "\n PERIOD: " + p + "\n";
            for (int vt = 0 ; vt < data.numberOfVehicleTypes ; vt++){
                out += "vehicle type " + vt + " take trips: ";
                for (Trip trip : tripList[p][vt]){
                    out += "trip " + trip.tripIndex + ": " + trip.customers + "\t";
                }
                out += "\n";
            }
        }
        return out;
    }

    public void setFitness(double fitness){ //USE WITH CARE. ONLY WHEN SETTING FITNESS FROM MIP
        this.fitness = fitness;
    }


    public void printDetailedFitness(){
        System.out.println("-------------------------------------");
        System.out.println("Fitness: " + fitness);
        System.out.println("Biased fitness: " + biasedFitness);
        System.out.println("Is feasbile: " + isFeasible());
        System.out.println("Fitness Rank:" + fitnessRank);
        System.out.println("Diversity Rank: "  +diversityRank);
        System.out.println("Diversity: " + diversity);

        System.out.println(" #Detailed cost#: ");
        System.out.println("Travel cost: " + travelCost);
        System.out.println("Vehicle usage cost: " + vehicleUsageCost);
        System.out.println("Order allocation cost: " + orderDistribution.fitness);
        System.out.println("Infeasibility cost: " + infeasibilityCost);

        System.out.println("True fitness (if feasible): " + Testing.IndividualTest.getTrueIndividualFitness(this));
        System.out.println("-------------------------------------");
    }

    public static Individual makeIndividual() {
        Data data = DataReader.loadData();
        OrderDistribution od = new OrderDistribution(data);
        od.makeInitialDistribution();
        Individual individual = new Individual(data);
        individual.initializeIndividual(od);
        for( int i = 0; i < 100; i++){
            AdSplit.adSplitPlural(individual);
            individual.updateFitness();
            individual.printDetailedFitness();
        }
        return individual;
    }

    public int compareTo(Individual individual) { // TODO: 04.03.2020 Sort by biased fitness and not fitness
        return this.getBiasedFitness() > individual.getBiasedFitness() ? 1 : -1;


    }
}








