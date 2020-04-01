package Master2020.DataFiles;


import gurobi.GRBVar;
import org.numenta.nupic.util.ArrayUtils;

import java.lang.*;
import java.util.Arrays;

public class Data {

    public Customer[] customers;
    public Vehicle[] vehicles;
    public Order[] orders;
    public Depot depot;
    public VehicleType[] vehicleTypes;  // todo: initialize

    public int numberOfPeriods = Parameters.numberOfPeriods;
    public int numberOfTrips = Parameters.numberOfTrips;
    public int numberOfDeliveries;
    public int numberOfCustomers = Parameters.numberOfCustomers;
    public int numberOfVehicles = Parameters.numberOfVehicles;
    public int numberOfNodes = Parameters.numberOfCustomers + 2;
    public int[] numberOfCustomerVisitsInPeriod;
    public int numberOfCustomerVisitsInPlanningHorizon;


    //Gurobi spesific variables
    public GRBVar[][][][][] arcs;

    public double totalVolume;
    public double targetVolume;

    public double[][] distanceMatrix;   //indexed by customer i and j

    //Derived parameters
    public int numberOfVehicleTypes;
    public int[] numberOfVehiclesInVehicleType;


    // Constructor
    public Data(Customer[] customers, Vehicle[] vehicles, Depot depot, VehicleType[] vehicleTypes){
        this.customers = customers;
        this.vehicles = vehicles;
        this.depot = depot;
        this.vehicleTypes = vehicleTypes;
        this.initialize();
    }

    private void initialize(){
        this.setVehiclesInVehicleType();
        this.setTargetVolume();
        this.setDistanceMatrix();
        setNearestNeighbors();
        this.setDerivedParameters();
        this.setNumberOfCustomerVisitsInPeriod();
        this.setCorrectOrderID();
    }

    private void setCorrectOrderID(){
        for (int customerID = 0; customerID < customers.length; customerID++){
            for (int i = 0; i < customers[customerID].orders.length; i++){
                this.customers[customerID].orders[i].setCustomerOrderID(i);
            }
            for (int i = 0; i < customers[customerID].dividableOrders.length; i++){
                this.customers[customerID].dividableOrders[i].setCustomerDividableOrderID(i);
            }
            for (int i = 0; i <  customers[customerID].nonDividableOrders.length; i++){
                this.customers[customerID].nonDividableOrders[i].setCustomerNonDividableOrderID(i);
            }
        }

    }

    private void setNumberOfCustomerVisitsInPeriod(){
        numberOfCustomerVisitsInPeriod = new int[numberOfPeriods];
        for (Customer customer : customers){
            for (int p = 0; p < numberOfPeriods; p++){
                this.numberOfCustomerVisitsInPeriod[p] += customer.requiredVisitPeriod[p];
                this.numberOfCustomerVisitsInPlanningHorizon += customer.requiredVisitPeriod[p];
            }
        }
    }

    private void setVehiclesInVehicleType(){
        for (Vehicle vehicle : vehicles){
            this.vehicleTypes[vehicle.vehicleType.vehicleTypeID].addVehicleToSet(vehicle.vehicleID);
        }
    }

    private void setDerivedParameters(){
        this.numberOfVehicleTypes = vehicleTypes.length;
        this.numberOfVehiclesInVehicleType = new int[numberOfVehicleTypes]; //initialized with zero
        for(Vehicle vehicle : vehicles){
            this.numberOfVehiclesInVehicleType[vehicle.vehicleType.vehicleTypeID]++;
        }
    }

    private void setNearestNeighbors(){
        double[] distanceCopy;
        int[] intDistanceCopy;
        int[] argSortedDistance;
        for (int customer = 0 ; customer < numberOfCustomers ; customer++){
            distanceCopy = ArrayUtils.multiply(Arrays.copyOf(distanceMatrix[customer], numberOfCustomers), 1000);
            intDistanceCopy = new int[distanceCopy.length];
            for (int i = 0; i < distanceCopy.length ; i++){
                intDistanceCopy[i] = (int) distanceCopy[i];
            }
            argSortedDistance = ArrayUtils.argsort(intDistanceCopy);
            for (int n = 1 ; n <= Math.min(numberOfCustomers - 1, Parameters.nearestNeighbors); n++){
                customers[customer].nearestNeighbors.add(customers[argSortedDistance[n]]);
            }

            //Diversity neighbours
            for (int n = 1 ; n <= Math.min(numberOfCustomers - 1, Parameters.nearestNeighborsDiversity); n++){
                customers[customer].nearestNeighborsDiversity.add(customers[argSortedDistance[n]]);
            }
        }

    }

    private void setDistanceMatrix() {
        distanceMatrix = new double[numberOfNodes][numberOfNodes];
        for (int i = 0; i < numberOfNodes; i++){
            for (int j = 0; j < numberOfNodes; j++){
                if ((i == customers.length || i == customers.length +1) &&
                        (j == customers.length || j == customers.length + 1)){
                    distanceMatrix[i][j] = 0;
                }
                else if (i == customers.length || i == customers.length +1) {
                    distanceMatrix[i][j] = euclideanDistance(depot.xCoordinate, depot.yCoordinate
                            , customers[j].xCoordinate, customers[j].yCoordinate)*Parameters.scalingDistanceParameter;
                }
                else if (j == customers.length || j == customers.length + 1){
                        distanceMatrix[i][j] = euclideanDistance(customers[i].xCoordinate, customers[i].yCoordinate,
                                depot.xCoordinate, depot.yCoordinate)*Parameters.scalingDistanceParameter;
                }
                else{
                    distanceMatrix[i][j] = euclideanDistance(customers[i].xCoordinate, customers[i].yCoordinate,
                            customers[j].xCoordinate, customers[j].yCoordinate)*Parameters.scalingDistanceParameter;
                }
            }
        }
    }

    private double euclideanDistance( double fromCustomerXCor, double formCustomerYCor,
                                      double toCustomerXCor, double toCustomerYCor){
        return Math.sqrt(Math.pow( (fromCustomerXCor-toCustomerXCor), 2 ) +
                Math.pow((formCustomerYCor - toCustomerYCor), 2));
    }


    private void setTargetVolume(){
        double totalVolume = 0;
        int numDeliveries = 0;
        for (Customer c : customers){
            for (Order o : c.orders){
                totalVolume += o.volume;
                numDeliveries ++;
            }
        }
        this.numberOfDeliveries = numDeliveries;
        this.totalVolume = totalVolume;
        this.orders = new Order[numDeliveries];
        for (Customer c : customers){
            for (Order o : c.orders){
                orders[o.orderID] = o;
            }
        }
        targetVolume = totalVolume/ numberOfPeriods;


    }


    public static void main(String[] args){
        Data data = DataReader.loadData();
        for (Customer c : data.customers[0].nearestNeighbors){
            System.out.println(c.customerID);
        }
        for (Customer c : data.customers[0].nearestNeighborsDiversity){
            System.out.println(c.customerID);
        }
    }

}
