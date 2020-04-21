package Master2020.Individual;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.DataFiles.Parameters;
import Master2020.Genetic.FitnessCalculation;
import Master2020.Population.Population;
import Master2020.ProductAllocation.OrderDistribution;
import Master2020.Testing.IndividualTest;

import java.util.ArrayList;
import java.util.HashMap;

public class PeriodicIndividual implements Comparable<Master2020.Individual.PeriodicIndividual> {

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

    private double fitness = Double.MAX_VALUE;
    private double diversity = -1;
    private double biasedFitness;

    private double diversityRank;
    private double fitnessRank;



    public PeriodicIndividual(Data data) {
        this.data = data;
        this.giantTour = new GiantTour(data);
        this.initializeTripMap();
        this.initializeTripList();
        this.initializeJourneyList();
    }


    public PeriodicIndividual(Data data, Population population) {
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
            AdSplit.adSplitPlural(null); //todo:change
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

    public void updateFitness(double penaltyMultiplier) {  //todo: change
        //Calculate objective costs
        double[] fitnesses = FitnessCalculation.getIndividualFitness(null, penaltyMultiplier); //// TODO: 21/04/2020 change 
        this.travelCost = fitnesses[0];
        this.infeasibilityCost = fitnesses[1] + fitnesses[2];
        this.vehicleUsageCost = fitnesses[3];
        this.timeWarpCost = fitnesses[1];
        this.overLoadCost = fitnesses[2];
        this.fitness = this.travelCost + this.vehicleUsageCost + this.timeWarpCost + this.overLoadCost + this.orderDistribution.getFitness();

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

    public void setFitness(double fitness){ //USE WITH CARE. ONLY WHEN SETTING FITNESS FROM Master2020.MIP
        this.fitness = fitness;
    }


    public void printDetailedFitness(){
        System.out.println("-------------------------------------");
        System.out.println("Individual - " + this.hashCode());
        System.out.println("Fitness: " + fitness);
        //System.out.println("True fitness: " + Master2020.Testing.IndividualTest.getTrueIndividualFitness(this)); // TODO: 21/04/2020 Implement for periodic individual
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

        /*
        Double trueFitness = IndividualTest.getTrueIndividualFitness(null);  //implement // TODO: 21/04/2020 Implement
        System.out.println("True fitness (if feasible): " + trueFitness);
        if ( Math.round(trueFitness*1000) != Math.round((travelCost+orderDistribution.fitness + vehicleUsageCost)*1000)){
            System.out.println("travel cost: " + travelCost);
            System.out.println("orderdistribution fitness: " + orderDistribution.fitness);
            System.out.println("Vehicle usage: " + vehicleUsageCost);;
            System.out.println("Test value 1: " +  Math.round(trueFitness*1000));
            System.out.println("Test value 2: " + Math.round((travelCost+orderDistribution.fitness + vehicleUsageCost)*1000));
            System.out.println("Sjekk individ");

        }

         */
        System.out.println("-------------------------------------");
    }

    public static Master2020.Individual.Individual makeIndividual() {
        Data data = DataReader.loadData();
        OrderDistribution od = new OrderDistribution(data);
        od.makeInitialDistribution();
        Master2020.Individual.Individual individual = new Master2020.Individual.Individual(data);
        individual.initializeIndividual(od);
        for( int i = 0; i < 100; i++){
            AdSplit.adSplitPlural(individual);
            individual.updateFitness();
            individual.printDetailedFitness();
        }
        return individual;
    }

    public int compareTo(Master2020.Individual.PeriodicIndividual periodicIndividual) { // TODO: 04.03.2020 Sort by biased fitness and not fitness
        return this.getBiasedFitness() > periodicIndividual.getBiasedFitness() ? 1 : -1;


    }
}

