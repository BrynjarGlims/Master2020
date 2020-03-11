package Individual;
import DataFiles.*;
import Population.Population;
import ProductAllocation.OrderDistribution;

import java.util.ArrayList;
import java.util.HashMap;


public class Individual implements Comparable<Individual> {
    //chromosomes
    public GiantTour giantTour;  //period, vehicleType
    public VehicleAssigment vehicleAssigment;
    public GiantTourSplit giantTourSplit;
    public OrderDistribution orderDistribution;
    public Population population;
    public CustomerToTrip[][] customerToTrips; //period, customer
    public HashMap< Integer, HashMap<Integer, Trip>> tripMap; //period, customer => trip
    public ArrayList<Trip>[][] tripList; //period, vehicleType

    public Data data;
    public boolean validCapacity;

    public double infeasibilityOvertimeDrivngValue;
    public double infeasibilityTimeWarpValue;
    public double infeasibilityOverCapacityValue;
    public double feasibleTravelingCost;
    public double feasibleVehicleUseCost;
    public double feasibleOvertimeDepotCost;

    public Label[][] bestLabels;

    //// TODO: 18.02.2020 TO be removed
    public Population Population;

    //fitness values:
    public double objectiveCost;
    public double infeasibilityCost;

    public double fitness = Double.MAX_VALUE;
    public double diversity = 0;
    public double biasedFitness;

    public boolean isSurvivor;


    public Individual(Data data) {
        this.data = data;
        this.vehicleAssigment = new VehicleAssigment(data);
        this.giantTourSplit = new GiantTourSplit(data);
        this.giantTour = new GiantTour(data);
        this.bestLabels = new Label[data.numberOfPeriods][data.numberOfVehicleTypes];
        this.customerToTrips = new CustomerToTrip[data.numberOfPeriods][data.numberOfCustomers];
        this.initializeTripMap();
        this.initializeTripList();
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



    public void initializeIndividual(OrderDistribution od) {
        //set chromosome
        this.orderDistribution = od;
        giantTourSplit.initialize();
        giantTour.initializeGiantTour();

        this.infeasibilityOverCapacityValue = 0;
        this.infeasibilityOvertimeDrivngValue = 0;
        this.infeasibilityTimeWarpValue = 0;

    }

    public void makeCustomerToTripMap(){
        for (int period = 0 ; period < data.numberOfPeriods ; period++){
            for (int vt = 0 ; vt < data.numberOfVehicleTypes ; vt++){
                makeCustomerToTripMapSingular(period, vt);
            }
        }
    }

    public void makeCustomerToTripMapSingular(int period, int vt){
        int tripCounter = 0;
        CustomerToTrip ctt;
        for (Trip trip : tripList[period][vt]){
            for (int customer = 0 ; customer < trip.customers.size() ; customer++){
                ctt = new CustomerToTrip(period, vt, trip.customers.get(customer), customer, trip, tripCounter);
                customerToTrips[period][trip.customers.get(customer)] = ctt;
            }
            tripCounter++;
        }
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

    public void testNewOrderDistribution(OrderDistribution orderDistribution){
        if (orderDistribution.equals(this.orderDistribution)){ // TODO: 06.03.2020 Remove print
            return;
        }
        double currentFitness = this.getFitness(false);
        //System.out.println("Fitness before adsplit: " + currentFitness );
        //this.printDetailedFitness();
        //System.out.println("Order distribution: " + this.orderDistribution.hashCode());
        OrderDistribution currentOrderDistribution = this.orderDistribution;
        this.setOptimalOrderDistribution(orderDistribution);
        //System.out.println("Fitness on new individual: " + this.getFitness(false) );
        //this.printDetailedFitness();
        //System.out.println("Order distribution: " + this.orderDistribution.hashCode());

        if (this.getFitness(false) > currentFitness){  // // TODO: 05.03.2020 Make more efficient
            this.setOptimalOrderDistribution(currentOrderDistribution);  //NOT WORKING

            //System.out.println("Fitness after adsplit: " + this.getFitness(false) );
            //this.printDetailedFitness();
            //System.out.println("Order distribution: " + this.orderDistribution.hashCode());


        }
        else{
            //System.out.println("%%%%%%%%%%%%%%%%% NEW BEST OD FOUND %%%%%%%%%%%%%%%%%%%%%%");
        }
        //System.out.println("###############################");
    }

    public void printDetailedFitness(){
        System.out.println("-------------------------------------");
        System.out.println("Biased fitness: " + biasedFitness);
        System.out.println("Diversity: " + diversity);
        System.out.println("Fitness: " + fitness);
        System.out.println("InfOvertimeValue: " + infeasibilityOvertimeDrivngValue);
        System.out.println("InfTimeWarp: " + infeasibilityTimeWarpValue);
        System.out.println("InfOverCapacityValue: " + infeasibilityOverCapacityValue);
        System.out.println("Objective cost: " + objectiveCost);
        System.out.println("Traveling cost: " + feasibleTravelingCost);
        System.out.println("Vehicle cost: " + feasibleVehicleUseCost);
        System.out.println("OvertimeAtDepot: " + feasibleOvertimeDepotCost);



        System.out.println("-------------------------------------");




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

    public double getFitness(boolean update) {
        if (update || this.fitness == Double.MAX_VALUE) {
            updateFitness();
            return fitness;
        } else {
            return fitness;
        }
    }


    public void updateFitness() {
        this.fitness = 0;

        //Calculate objective costs
        this.objectiveCost = getObjectiveCost();

        //Add infeasibility costs
        this.infeasibilityCost = getInfeasibilityCost();

        this.fitness = this.objectiveCost + this.infeasibilityCost;
        
        //// TODO: 05.03.2020 Move this to another place when diversity is implemented 
        this.biasedFitness = fitness + diversity;

    }

    private double getObjectiveCost() {
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


    private double getInfeasibilityCost() {
        infeasibilityTimeWarpValue = 0;
        infeasibilityOverCapacityValue = 0;
        infeasibilityOvertimeDrivngValue = 0;
        for (Label[] labels : bestLabels) {
            for (Label label : labels) {
                if (label.isEmptyLabel) {
                    continue;
                }
                //Already added scaling parameters in label
                infeasibilityTimeWarpValue += label.getTimeWarpInfeasibility();
                infeasibilityOverCapacityValue += label.getLoadInfeasibility();
                infeasibilityOvertimeDrivngValue += label.getOvertimeInfeasibility();
            }
        }
        return infeasibilityOvertimeDrivngValue + infeasibilityOverCapacityValue + infeasibilityTimeWarpValue;
    }

    public double getIndividualBiasedFitnessScore() {
        fitness = 0.0; //TODO: implement fitness calculations
        //calculate biased fitness element
        int nbIndividuals = 0;
        if (this.isFeasible()) {
            nbIndividuals = Population.getSizeOfFeasiblePopulation();
        } else if (!this.isFeasible()) {
            nbIndividuals = Population.getSizeOfInfeasiblePopulation();
        }
        double biasedFitness = (1 - (Parameters.numberOfEliteIndividuals / nbIndividuals) * getRankOfIndividual());
        double fitnessScore = fitness + biasedFitness;
        return fitnessScore;
    }

    public double getBiasedFitness(){
        return this.getFitness(false) - this.diversity;
    }

    public double calculateDiversity(Individual comparison) {

        return 0;

    }

    public double hammingDistance(GiantTour gt) {
        double customerDistance = 0;
        double vehicleTypeDistance = 0;
        int vt1 = 0;
        int vt2 = 0;
        int counter1 = 0;
        int counter2 = 0;
        for (int p = 0; p < data.numberOfPeriods; p++) {
            for (int c = 0; c < data.numberOfCustomerVisitsInPeriod[p]; c++) {
                if (gt.chromosome[p][vt2].size() == counter2) {
                    counter2 = 0;
                    vt2++;
                }
                if (this.giantTour.chromosome[p][vt1].size() - 1 == counter1) {
                    counter1 = 0;
                    vt1++;
                }
                customerDistance += (this.giantTour.chromosome[p][vt1].get(counter1) != gt.chromosome[p][vt2].get(counter2)) ? 1 : 0;
                vehicleTypeDistance += (vt2 != vt1) ? 1 : 0;
                counter1++;
                counter2++;
            }

        }
        customerDistance /= 2 * data.numberOfCustomerVisitsInPlanningHorizon;

        // TODO: 26.02.2020 Check if the proportional customer distance and vehicle type distance
        return customerDistance + vehicleTypeDistance; //larger distance, more diversity
    }

    public String toString(){
        return giantTour.toString();
    }

    public static void main(String[] args) {
        Individual individual = Individual.makeIndividual();
        //System.out.println("Value of fitness: " + individual.getFitness(true));

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









