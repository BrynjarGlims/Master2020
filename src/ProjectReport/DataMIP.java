package ProjectReport;
import gurobi.GRBVar;

import javax.swing.text.DefaultEditorKit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import ProjectReport.*;


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
    int numProducts;
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
    int costOvertime; // constant
    int[] costVehicle; // [v] constant per vehicle

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

    public void initialize(){
        numNodes = numCustomers + 2;
        numProducts = customers[0].productTypes.length;

        products = new double[numCustomers];
        fixedUnloadingTime = new double[numCustomers];
        for (int i = 0; i < numCustomers ; i++){
            products[i] = Arrays.stream(customers[i].productQuantities).sum();
            fixedUnloadingTime[i] = customers[i].fixedUnloadingTime;
        }

        productTypes = new int[numCustomers][numProducts];
        productQuantity = new double[numCustomers][numProducts];
        for (int customer = 0 ; customer < numCustomers; customer++){
            if (numProducts >= 0)
                System.arraycopy(customers[customer].productTypes, 0, productTypes[customer], 0, numProducts);
                System.arraycopy(customers[customer].productQuantities, 0, productQuantity[customer], 0, numProducts);
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

        minFrequencyProduct = new int[numCustomers][numProducts];
        maxFrequencyProduct = new int[numCustomers][numProducts];
        maxAmountDivProduct = new double[numCustomers][numProducts];
        minAmountDivProduct = new double[numCustomers][numProducts];
        for (int customer = 0; customer < numCustomers; customer++){
            for (int product = 0; product < numProducts-5; product++){  //TODO: Endre slik at man får dynamisk lengde på produkt. 5 = numNonDivProduuct
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
    }





    public double getDistance(Customer c1, Customer c2){
        return travelTime[c1.customerID][c2.customerID];
    }
}
