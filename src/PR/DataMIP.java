package PR;
import DataFiles.Data;
import DataFiles.Parameters;
import MIP.DataConverter;
import gurobi.GRBVar;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;


public class DataMIP {

    String instanceName;

    Customer[] customers;
    Vehicle[] vehicles;
    VehicleType[] vehicleTypes;
    double xCoordinateDepot;
    double yCoordinateDepot;
    double latestInTime;
    GRBVar[][][][][] arcs;
    GRBVar[][][][] paths;

    Map<Integer, Map<Integer, HashSet<Path>>> pathMap;
    Map<Integer, Map<Integer, HashSet<Journey>>> journeyMap;

    int numPeriods;
    int numTrips;
    int numCustomers;
    int[] numProductsPrCustomer;
    int numNodes;  //this should be 2 larger than customers, since we have a N+1 depot
    int numVehicles;
    int numVehicleTypes;

    double[] products;  // [i] total quantum of products for customer i
    int[][] productTypes; //  (i,m) // 1 is dividable and 0 is non dividable. first index is customer i, second is product m
    double[][] productQuantity; // (i,m) quantity of every product
    public static int upperBoundQuantity = 100;  // upper bound q variable
    public static int upperBoundOvertime = 1000;  // upper bound qO variable


    double[][] timeWindowStart; //(d, i)
    double[][] timeWindowEnd; //(d, i)
    double[] costOvertime; // constant
    double[] costVehicle; // [v] constant per vehicle

    double[] travelCost; //[v] unit cost per unit travelled by vehicle v
    double[] vehicleCapacity; // [v] capacity per vehicle
    double[] overtimeLimit; // [d] limit of products delivered day d before overtime is inferred
    double[][] maxAmountDivProduct; // (i,m) upper limit of dividable product m for customer i
    double[][] minAmountDivProduct; // (i,m) lower limit of dividable product m for customer i
    int[][] minFrequencyProduct; //(i,m) min amount of days to deliver a product
    int[][] maxFrequencyProduct; //(i,m) max amount of days to deliver a product

    // Time variables
    double[][] travelTime; // (i,j) travel time in matrix A
    double[] fixedUnloadingTime; //[i] fixed time per customer
    double[] loadingTime; //[vehicletypes] loading time at depot for vehicletypes

    int[][] possibleDeliveryDays; //(d,i) visit allowed on day d to customer i

    // Derived variables
    double maxVehicleCapacity = 0;
    double overtimeLimitAveraged;


    public void addCustomer(Customer customer){
        customers[customer.customerID] = customer;
    }
    public void addVehicle(Vehicle vehicle){ vehicles[vehicle.vehicleID] = vehicle; }
    public void addVehicleType(VehicleType vehicleType){
        vehicleTypes[vehicleType.type] = vehicleType;
    }
    public void setDistances(double[][] distances){
        this.travelTime = distances;
    }
    public void setPathMap(Map<Integer, Map<Integer, HashSet<Path>>> pathMap){
        this.pathMap = pathMap;
    }
    public void setJourneyMap(Map<Integer, Map<Integer, HashSet<Journey>>> journeyMap){
        this.journeyMap = journeyMap;
    }


    public void storeMainData ( Customer[] customers, VehicleType[] vehicleTypes, Vehicle[] vehicles){
        this.customers = customers;
        this.vehicleTypes = vehicleTypes;
        this.vehicles = vehicles;
    }

    public void storeAdditionalData(Data data){
        numNodes = data.numberOfCustomers + 2;
        numPeriods = Parameters.numberOfPeriods;
        numProductsPrCustomer = DataConverter.getNumProductsPrCustomer(data.customers);
        numCustomers = customers.length;
        numTrips = Parameters.numberOfTrips;
        numVehicleTypes = vehicleTypes.length;
        numVehicles = vehicles.length;
        xCoordinateDepot = data.depot.xCoordinate;
        yCoordinateDepot = data.depot.yCoordinate;

        products = new double[numCustomers];
        fixedUnloadingTime = new double[numCustomers];

        for (int i = 0; i < numCustomers ; i++){
            products[i] = Arrays.stream(customers[i].productQuantities).sum();
            fixedUnloadingTime[i] = customers[i].fixedUnloadingTime;
        }

        productTypes = new int[numCustomers][];
        productQuantity = new double[numCustomers][];

        for (int i = 0; i < numCustomers; i++){
            productTypes[i] = new int[numProductsPrCustomer[i]];
            productQuantity[i] = new double[numProductsPrCustomer[i]];
        }




        for (int customer = 0 ; customer < numCustomers; customer++){
            if (numProductsPrCustomer[customer] >= 0)
                System.arraycopy(customers[customer].productTypes, 0, productTypes[customer], 0, numProductsPrCustomer[customer]);
            System.arraycopy(customers[customer].productQuantities, 0, productQuantity[customer], 0, numProductsPrCustomer[customer]);
        }

        timeWindowStart = new double[numPeriods][numCustomers];
        timeWindowEnd = new double[numPeriods][numCustomers];
        possibleDeliveryDays = new int[numPeriods][numCustomers];
        for (int day = 0 ; day < numPeriods ; day++){
            for (int customer = 0 ; customer < numCustomers ; customer++){

                if (customers[customer].visitDays[day] == 1){
                    timeWindowStart[day][customer] = customers[customer].getTimeWindow(day)[0];
                    timeWindowEnd[day][customer] = customers[customer].getTimeWindow(day)[1];
                    possibleDeliveryDays[day][customer] = customers[customer].visitDays[day];
                }
            }
        }

        minFrequencyProduct = new int[numCustomers][];
        maxFrequencyProduct = new int[numCustomers][];
        maxAmountDivProduct = new double[numCustomers][];
        minAmountDivProduct = new double[numCustomers][];
        for (int i = 0; i < numCustomers; i++){
            minFrequencyProduct[i] = new int[numProductsPrCustomer[i]];
            maxFrequencyProduct[i] = new int[numProductsPrCustomer[i]];
            maxAmountDivProduct[i] = new double[numProductsPrCustomer[i]];
            minAmountDivProduct[i] = new double[numProductsPrCustomer[i]];
        }
        for (int customer = 0; customer < numCustomers; customer++){
            for (int product = 0; product < numProductsPrCustomer[customer]; product++){  //TODO: Endre slik at man får dynamisk lengde på produkt. 5 = numNonDivProduuct
                minFrequencyProduct[customer][product] = customers[customer].minFrequencyProduct[product];
                maxFrequencyProduct[customer][product] = customers[customer].maxFrequencyProduct[product];
                minAmountDivProduct[customer][product] = customers[customer].minQuantityProduct[product];
                maxAmountDivProduct[customer][product] = customers[customer].maxQuantityProduct[product];

            }
        }

        costVehicle = new double[numVehicles];
        travelCost = new double[numVehicles];
        vehicleCapacity = new double[numVehicles];

        for (Vehicle vehicle : vehicles){
            costVehicle[vehicle.vehicleID] = vehicle.vehicleType.unitCost;
            travelCost[vehicle.vehicleID] = vehicle.vehicleType.drivingCost;
            vehicleCapacity[vehicle.vehicleID] = vehicle.vehicleType.capacity;    }

        loadingTime = new double[numVehicleTypes];
        for (VehicleType vehicletype : vehicleTypes){
            loadingTime[vehicletype.type] = vehicletype.loadingTime;
        }

        for( double capacity : vehicleCapacity){
            if (capacity > maxVehicleCapacity)
                maxVehicleCapacity = capacity;
        }

        double sumProducts = 0;
        for (Customer c : customers){
            for (double volume : c.productQuantities){
                sumProducts += volume;
            }
        }
        overtimeLimitAveraged = sumProducts/numPeriods;
        calculateTravelTime();
        overtimeLimit = Parameters.overtimeLimit;
        costOvertime = Parameters.overtimeCost;
    }


    public void initialize(){

        //todo: FIX
        /*
        numNodes = numCustomers + 2;
        numProductsPrCustomer = customers[0].productTypes.length;

        products = new double[numCustomers];
        fixedUnloadingTime = new double[numCustomers];
        for (int i = 0; i < numCustomers ; i++){
            products[i] = Arrays.stream(customers[i].productQuantities).sum();
            fixedUnloadingTime[i] = customers[i].fixedUnloadingTime;
        }

        productTypes = new int[numCustomers][numProductsPrCustomer];
        productQuantity = new double[numCustomers][numProductsPrCustomer];
        for (int customer = 0 ; customer < numCustomers; customer++){
            if (numProductsPrCustomer >= 0)
                System.arraycopy(customers[customer].productTypes, 0, productTypes[customer], 0, numProductsPrCustomer);
                System.arraycopy(customers[customer].productQuantities, 0, productQuantity[customer], 0, numProductsPrCustomer);
        }

        timeWindowStart = new double[numPeriods][numCustomers];
        timeWindowEnd = new double[numPeriods][numCustomers];
        possibleDeliveryDays = new int[numPeriods][numCustomers];
        for (int day = 0 ; day < numPeriods ; day++){
            for (int customer = 0 ; customer < numCustomers ; customer++){

                if (customers[customer].visitDays[day] == 1){
                    timeWindowStart[day][customer] = customers[customer].getTimeWindow(day)[0];
                    timeWindowEnd[day][customer] = customers[customer].getTimeWindow(day)[1];
                    possibleDeliveryDays[day][customer] = customers[customer].visitDays[day];
                }
            }
        }

        minFrequencyProduct = new int[numCustomers][numProductsPrCustomer];
        maxFrequencyProduct = new int[numCustomers][numProductsPrCustomer];
        maxAmountDivProduct = new double[numCustomers][numProductsPrCustomer];
        minAmountDivProduct = new double[numCustomers][numProductsPrCustomer];
        for (int customer = 0; customer < numCustomers; customer++){
            for (int product = 0; product < numProductsPrCustomer -5; product++){  //TODO: Endre slik at man får dynamisk lengde på produkt. 5 = numNonDivProduuct
                minFrequencyProduct[customer][product] = customers[customer].minFrequencyProduct[product];
                maxFrequencyProduct[customer][product] = customers[customer].maxFrequencyProduct[product];
                minAmountDivProduct[customer][product] = customers[customer].minQuantityProduct[product];
                maxAmountDivProduct[customer][product] = customers[customer].maxQuantityProduct[product];
            }
        }

        costVehicle = new int[numVehicles];
        travelCost = new double[numVehicles];
        vehicleCapacity = new double[numVehicles];

        for (Vehicle vehicle : vehicles){
            costVehicle[vehicle.vehicleID] = vehicle.vehicleType.unitCost;
            travelCost[vehicle.vehicleID] = vehicle.vehicleType.drivingCost;
            vehicleCapacity[vehicle.vehicleID] = vehicle.vehicleType.capacity;    }

        loadingTime = new double[numVehicleTypes];
        for (VehicleType vehicletype : vehicleTypes){
            loadingTime[vehicletype.type] = vehicletype.loadingTime;
        }

        for( double capacity : vehicleCapacity){
            if (capacity > maxVehicleCapacity)
                maxVehicleCapacity = capacity;
        }

        double sumProducts = 0;
        for (Customer c : customers){
            for (double volume : c.productQuantities){
                sumProducts += volume;
            }
        }
        overtimeLimitAveraged = sumProducts/numPeriods;

        
         */

    }

    private void calculateTravelTime() {


        travelTime = new double[numNodes][numNodes];
        for (int i = 0; i < numNodes; i++) {
            for (int j = 0; j < numNodes; j++) {
                if (i >= numCustomers && j >= numCustomers) {
                    travelTime[i][j] = 0;
                } else if (i < numCustomers && j >= numCustomers) {
                    travelTime[i][j] = Math.sqrt(Math.pow(customers[i].xCoordinate - this.xCoordinateDepot, 2)
                            + Math.pow(customers[i].yCoordinate - this.yCoordinateDepot, 2));
                } else if (j < numCustomers && i >= numCustomers) {
                    travelTime[i][j] = Math.sqrt(Math.pow(customers[j].xCoordinate - this.xCoordinateDepot, 2)
                            + Math.pow(customers[j].yCoordinate - this.yCoordinateDepot, 2));
                } else {
                    travelTime[i][j] = Math.sqrt(Math.pow(customers[i].xCoordinate - customers[j].xCoordinate, 2)
                            + Math.pow(customers[i].yCoordinate - customers[j].yCoordinate, 2));

                }
            }
        }
    }





    public double getDistance(Customer c1, Customer c2){
        return travelTime[c1.customerID][c2.customerID];
    }
}
