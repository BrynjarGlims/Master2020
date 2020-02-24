package DataFiles;


import gurobi.GRBVar;

public class Data {

    public Customer[] customers;
    public Vehicle[] vehicles;
    public Depot depot;
    public VehicleType[] vehicleTypes;  // todo: initialize

    public int numberOfPeriods = Parameters.numberOfPeriods;
    public int numberOfTrips = Parameters.numberOfTrips;
    public int numberOfDeliveries;
    public int numberOfCustomers = Parameters.numberOfCustomers;
    public int numberOfVehicles = Parameters.numberOfVehicles;
    public int numberOfNodes = Parameters.numberOfCustomers + 2;


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
        this.setDerivedParameters();
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
            for (Order p : c.orders){
                totalVolume += p.volume;
                numDeliveries ++;
            }
        }
        this.numberOfDeliveries = numDeliveries;
        this.totalVolume = totalVolume;
        targetVolume = totalVolume/ numberOfPeriods;

    }


    public static void main(String[] args){
        Data data = DataReader.loadData();
        for (Customer c : data.customers){
            if (c.timeWindow.length != 6){
                System.out.println(c.customerID);
            }
        }
        }

}
