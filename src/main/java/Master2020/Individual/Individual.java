package Master2020.Individual;
import Master2020.DataFiles.*;
import Master2020.Genetic.FitnessCalculation;
import Master2020.Genetic.PenaltyControl;
import Master2020.Population.Population;
import Master2020.ProductAllocation.OrderDistribution;
import Master2020.Testing.IndividualTest;
import Master2020.Individual.GiantTour;

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

    //fitness values:
    public double travelCost;
    public double infeasibilityCost;
    public double vehicleUsageCost;
    public double timeWarpCost;
    public double overLoadCost;
    public double periodicCost; // all costs except orderDistribution cost

    private double fitness = Double.MAX_VALUE;
    private double diversity = -1;
    private double biasedFitness;

    private double diversityRank;
    private double fitnessRank;
    public boolean isSurvivor;

    public boolean isPeriodic;
    public int numberOfPeriods;
    public int actualPeriod;

    public PenaltyControl penaltyControl;


    public Individual(Data data, PenaltyControl penaltyControl) {
        this(data, null, false,  -1, penaltyControl);
    }

    public Individual(Data data, Population population, PenaltyControl penaltyControl) {
        this(data, population, false, -1, penaltyControl);

    }

    public Individual(Data data, Population population, boolean isPeriodic, int actualPeriod, PenaltyControl penaltyControl) {
        this.data = data;
        this.isPeriodic = isPeriodic;
        this.numberOfPeriods = (isPeriodic) ? 1 : data.numberOfPeriods;
        if (isPeriodic){
            this.actualPeriod = actualPeriod;
        }
        this.giantTour = new GiantTour(data, this.isPeriodic, this.actualPeriod);
        this.initializeTripMap();
        this.initializeTripList();
        this.initializeJourneyList();
        this.population = population;
        this.penaltyControl = penaltyControl;

    }

    public void setJourneyList(ArrayList<Journey> journeyList, int period, int vehicleType){
        this.journeyList[period][vehicleType] = journeyList;
    }



    public  void initializeTripList(){
        this.tripList = new ArrayList[this.numberOfPeriods][data.numberOfVehicleTypes];
        for (int p = 0 ; p < this.numberOfPeriods; p++){
            for (int vt = 0; vt < data.numberOfVehicleTypes; vt++){
                this.tripList[p][vt] = new ArrayList<Trip>();
            }
        }
    }

    public void initializeTripMap(){
        this.tripMap = new HashMap<Integer, HashMap<Integer, Trip>>();
        for (int p = 0; p < this.numberOfPeriods; p++){
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
        this.journeyList = new ArrayList[this.numberOfPeriods][data.numberOfVehicleTypes];
        for (int p = 0 ; p < this.numberOfPeriods; p++){
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


    public void setOptimalOrderDistribution(OrderDistribution orderDistribution) {
        this.orderDistribution = orderDistribution;
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

    public void setGiantTourFromJourneys(){
        //updates giantTour chromosome from trips changed in education
        GiantTour gt = new GiantTour(data, isPeriodic, actualPeriod);
        for (int period = 0 ; period < this.numberOfPeriods ; period++){
            for (int vehicleType = 0 ; vehicleType < data.numberOfVehicleTypes ; vehicleType++){
                setGiantTourFromJourneysPerPeriodVehicleType(period, vehicleType, gt);
            }
        }
        this.giantTour = gt;
    }

    public void setTripListFromJourneys(){
        for (int p = 0; p < this.numberOfPeriods; p++ ){
            for (int vt  = 0; vt < data.numberOfVehicleTypes; vt++){
                for (Journey journey : this.journeyList[p][vt]){
                    for (Trip trip : journey.trips){
                        this.tripList[p][vt].add(trip);
                    }
                }
            }
        }
    }

    private void setGiantTourFromJourneysPerPeriodVehicleType(int p, int vt, GiantTour gt) {
        ArrayList<Integer> giantTourEntry = new ArrayList<>();
        gt.chromosome[p][vt] = giantTourEntry;
        for (Journey journey : journeyList[p][vt]) {
            tripList[p][vt].addAll(journey.trips);
            for (Trip trip : journey.trips) {
                giantTourEntry.addAll(trip.customers);
            }
        }
    }


    public void setGiantTourFromTrips(){
        //updates giantTour chromosome from trips changed in education
        GiantTour gt = new GiantTour(data, isPeriodic, actualPeriod);
        for (int period = 0 ; period < this.numberOfPeriods ; period++){
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
        double[] fitnesses = FitnessCalculation.getIndividualFitness(this, penaltyMultiplier, penaltyControl.timeWarpPenalty, penaltyControl.overLoadPenalty);
        this.travelCost = fitnesses[0];
        this.infeasibilityCost = fitnesses[1] + fitnesses[2];
        this.vehicleUsageCost = fitnesses[3];
        this.timeWarpCost = fitnesses[1];
        this.overLoadCost = fitnesses[2];
        this.fitness = this.travelCost + this.vehicleUsageCost + this.timeWarpCost + this.overLoadCost + this.orderDistribution.getFitness();
    }

    public double getPeriodicCost() {
        return getPeriodicCost(1);
    }

    public double getPeriodicCost(double penaltyMultiplier) {
        //Calculate objective costs
        double[] fitnesses = FitnessCalculation.getIndividualFitness(this, penaltyMultiplier, penaltyControl.timeWarpPenalty, penaltyControl.overLoadPenalty);
        this.travelCost = fitnesses[0];
        this.infeasibilityCost = fitnesses[1] + fitnesses[2];
        this.vehicleUsageCost = fitnesses[3];
        this.timeWarpCost = fitnesses[1];
        this.overLoadCost = fitnesses[2];
        this.fitness = this.travelCost + this.vehicleUsageCost + this.timeWarpCost + this.overLoadCost + this.orderDistribution.getFitness();
        this.periodicCost = this.infeasibilityCost + this.travelCost + this.vehicleUsageCost;
        return this.periodicCost;
    }


    public double getBiasedFitness() {
        return biasedFitness;
    }

    public int getActualPeriod(int period){
        if (Parameters.isPeriodic){
            return this.actualPeriod;
        }
        else {
            return period;
        }
    }

    public void calculateBiasedFitness(){
        double diversityScaling;
        if (this.isFeasible()){
            diversityScaling = 1.0 - ((double) Parameters.populationSize*Parameters.fractionEliteIndividuals / (double) this.population.feasiblePopulation.size());
        }
        else{
            diversityScaling = 1.0 - ((double) Parameters.populationSize*Parameters.fractionEliteIndividuals/ (double) this.population.infeasiblePopulation.size());
        }
        biasedFitness = (double) fitnessRank + (diversityScaling * (double) diversityRank);
    }

    public boolean hasOverLoad(){
        return this.overLoadCost > 0;
    }

    public boolean hasTimeWarp(){
        return this.timeWarpCost > 0;
    }


    public void setDiversity(double diversity){
        this.diversity = diversity;
    }

    public String toString(){
        String out = "";
        for (int p = 0 ; p < this.numberOfPeriods ; p++){
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

    public void setTripMapFromTripList(){
        tripMap = new HashMap< Integer, HashMap<Integer, Trip>>();
        for (int p = 0; p < numberOfPeriods; p++){
            this.tripMap.put(p,new HashMap<Integer, Trip>());
            for (int vt = 0; vt < data.numberOfVehicleTypes; vt++){
                for (Trip trip : this.tripList[p][vt]){
                    for (int customerID : trip.customers){
                        this.tripMap.get(p).put(customerID, trip);
                    }
                }
            }
        }
    }


    public void setFitness(double fitness){ //USE WITH CARE. ONLY WHEN SETTING FITNESS FROM Master2020.MIP
        this.fitness = fitness;
    }


    public void printDetailedFitness(){
        System.out.println("-------------------------------------");
        System.out.println("Individual - " + this.hashCode());
        System.out.println("Fitness: " + fitness);
        System.out.println("True fitness: " + Master2020.Testing.IndividualTest.getTrueIndividualFitness(this));
        System.out.println("Is feasbile: " + isFeasible());
        System.out.println("Infeasibility cost2: " + infeasibilityCost);
        System.out.println("Biased fitness: " + biasedFitness);
        System.out.println("Diversity Rank: "  +diversityRank);
        System.out.println("Diversity: " + diversity);
        System.out.println(" #Detailed cost# ");
        System.out.println("Travel cost: " + travelCost);
        System.out.println("Vehicle usage cost: " + vehicleUsageCost);
        System.out.println("Order allocation cost: " + orderDistribution.fitness);
        System.out.println("Time warp cost: " + timeWarpCost);
        System.out.println("Over load cost: " + overLoadCost);

        Double trueFitness = IndividualTest.getTrueIndividualFitness(this);
        System.out.println("True fitness (if feasible): " + trueFitness);
        if ( Math.round(trueFitness*1000) != Math.round((travelCost+orderDistribution.fitness + vehicleUsageCost)*1000)){
            System.out.println("travel cost: " + travelCost);
            System.out.println("orderdistribution fitness: " + orderDistribution.fitness);
            System.out.println("Vehicle usage: " + vehicleUsageCost);;
            System.out.println("Test value 1: " +  Math.round(trueFitness*1000));
            System.out.println("Test value 2: " + Math.round((travelCost+orderDistribution.fitness + vehicleUsageCost)*1000));
            System.out.println("Sjekk individ");

        }
        System.out.println("-------------------------------------");
    }

    public static int getDefaultPeriod(int period){
        return Parameters.isPeriodic ? 0 : period;
    }

    public static Individual makeIndividual() {
        Data data = DataReader.loadData();
        OrderDistribution od = new OrderDistribution(data);
        od.makeInitialDistribution();
        PenaltyControl penaltyControl = new PenaltyControl(Parameters.initialTimeWarpPenalty, Parameters.initialOverLoadPenalty);
        Individual individual = new Individual(data, penaltyControl);
        individual.initializeIndividual(od);
        for( int i = 0; i < 100; i++){
            AdSplit.adSplitPlural(individual, Parameters.initialTimeWarpPenalty, Parameters.initialOverLoadPenalty);
            individual.updateFitness();
            individual.printDetailedFitness();
        }
        return individual;
    }

    public int compareTo(Individual individual) {
        if (this.getBiasedFitness() == individual.getBiasedFitness()){ // if tie, make a consistent choice
            return (this.hashCode() < individual.hashCode()) ? -1 : 1;
        }
        return this.getBiasedFitness() > individual.getBiasedFitness() ? 1 : -1;
    }
}








