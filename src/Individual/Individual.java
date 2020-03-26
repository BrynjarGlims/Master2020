package Individual;
import DataFiles.*;
import MIP.ArcFlowModel;
import Population.Population;
import ProductAllocation.OrderDistribution;
import scala.xml.PrettyPrinter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;


public class Individual implements Comparable<Individual> {
    //chromosomes
    public GiantTour giantTour;  //period, vehicleType
    public OrderDistribution orderDistribution;
    public Population population;
    public HashMap< Integer, HashMap<Integer, Trip>> tripMap; //period, customer => trip
    public ArrayList<Trip>[][] tripList; //period, vehicleType
    public ArrayList<Journey>[][] journeyList; //period, vehicleType
    public Data data;

    public double infeasibilityOvertimeDrivngValue;
    public double infeasibilityTimeWarpValue;
    public double infeasibilityOverCapacityValue;
    public double feasibleTravelingCost;
    public double feasibleVehicleUseCost;
    public double feasibleOvertimeDepotCost;
    public Label[][] bestLabels;

    public Population Population;

    //fitness values:
    public double objectiveCost;
    public double infeasibilityCost;

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

    public  void initializeTripList(){
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

        this.infeasibilityOverCapacityValue = 0;
        this.infeasibilityOvertimeDrivngValue = 0;
        this.infeasibilityTimeWarpValue = 0;
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

    public void setOptimalOrderDistribution(OrderDistribution orderDistribution, boolean doAdSplit) {
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

    public void printDetailedFitness(){
        System.out.println("-------------------------------------");
        System.out.println("Fitness: " + fitness);
        System.out.println("True fitness: " + Testing.IndividualTest.getTrueIndividualFitness(this));
        System.out.println("Is feasbile: " + isFeasible());
        System.out.println("Biased fitness: " + biasedFitness);
        System.out.println("Diversity Rank: "  +diversityRank);
        System.out.println("Diversity: " + diversity);
        System.out.println("Fitness: " + fitness);
        System.out.println("Fitness Rank:" + fitnessRank);
        System.out.println("InfOvertimeValue: " + infeasibilityOvertimeDrivngValue);
        System.out.println("InfTimeWarp: " + infeasibilityTimeWarpValue);
        System.out.println("InfOverCapacityValue: " + infeasibilityOverCapacityValue);
        System.out.println("Objective cost: " + objectiveCost);
        System.out.println("Traveling cost: " + feasibleTravelingCost);
        System.out.println("Vehicle cost: " + feasibleVehicleUseCost);
        System.out.println("OvertimeAtDepot: " + feasibleOvertimeDepotCost);
        System.out.println("-------------------------------------");




    }

    public HashSet<String> getArcs(){
        HashSet<String> arcs = new HashSet<>();
        for (ArrayList<Journey>[] journeysPeriod : journeyList){
            for (ArrayList<Journey> journeys : journeysPeriod){
                for( Journey journey : journeys){
                    arcs.addAll(journey.getArcsUsed());
                }
            }
        }
        return arcs;
    }


    public boolean isFeasible() {
        return (infeasibilityCost == 0);

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
        this.objectiveCost = getObjectiveCost(penaltyMultiplier);

        //Add infeasibility costs
        this.infeasibilityCost = getInfeasibilityCost();

        this.fitness = this.objectiveCost + this.infeasibilityCost;
    }

    private double getObjectiveCost(){
        return getObjectiveCost(1);
    }

    private double getObjectiveCost(double penaltyMultiplier) {
        feasibleOvertimeDepotCost = 0;
        feasibleTravelingCost = 0;
        feasibleVehicleUseCost = 0;

        for (Label[] labels : bestLabels) {
            for (Label label : labels) {
                if (label.isEmptyLabel) {
                    continue;
                }
                //Adds driving cost
                feasibleTravelingCost += label.getLabelDrivingDistance() * data.vehicleTypes[label.vehicleTypeID].travelCost;
                //Adds vehicle use cost
                feasibleVehicleUseCost += label.getNumberOfVehicles() * data.vehicleTypes[label.vehicleTypeID].usageCost;
                // todo: Cost allready calculated
            }
        }
        feasibleOvertimeDepotCost += orderDistribution.getOvertimeValue();
        return feasibleTravelingCost + feasibleVehicleUseCost + feasibleOvertimeDepotCost;
    }

    private double getInfeasibilityCost(){
        return getInfeasibilityCost(1);
    }

    private double getInfeasibilityCost(double penaltyMultiplier) {
        infeasibilityTimeWarpValue = 0;
        infeasibilityOverCapacityValue = 0;
        infeasibilityOvertimeDrivngValue = 0;
        for (Label[] labels : bestLabels) {
            for (Label label : labels) {
                if (label.isEmptyLabel) {
                    continue;
                }
                //Already added scaling parameters in label
                infeasibilityTimeWarpValue += penaltyMultiplier*label.getTimeWarpInfeasibility();
                infeasibilityOverCapacityValue += penaltyMultiplier*label.getLoadInfeasibility();
                infeasibilityOvertimeDrivngValue += penaltyMultiplier*label.getOvertimeInfeasibility();
            }
        }
        return infeasibilityOvertimeDrivngValue + infeasibilityOverCapacityValue + infeasibilityTimeWarpValue;
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
        //System.out.println("FitnessRank: " + fitnessRank);
        //System.out.println("DiversityRank: " + diversityRank);
        //System.out.println("PopulationSize: " + this.population.getTotalPopulation().size());
        //System.out.println("Elite individuals: " + Parameters.numberOfElitismSurvivorsPerGeneration);
        //System.out.println("Diversity Scaling: " + diversityScaling);
        //System.out.println("Biased fitness: " + biasedFitness);
        //System.out.println("-------------------------");
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

    public static void main(String[] args) {
        Individual individual = Individual.makeIndividual();
        //System.out.println("Value of fitness: " + individual.getFitness(true));

    }

    public void setFitness(double fitness){ //USE WITH CARE. ONLY WHEN SETTING FITNESS FROM MIP
        this.fitness = fitness;
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
        if (this.getBiasedFitness() < individual.getBiasedFitness() ) {
            return 1;
        }
        else {
            return -1;
        }

    }


}








