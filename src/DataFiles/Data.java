package DataFiles;
import java.lang.Math.*;


public class Data {

    public Customer[] customers;
    public Vehicle[] vehicles;
    public Depot depot;

    public int numPeriods;
    public int numDeliveries;

    public double totalVolume;
    public double targetVolume;

    public double[][] distanceMatrix;   //indexed by customer i and j

    public Data(Customer[] customers, Vehicle[] vehicles, Depot depot){
        this.customers = customers;
        this.vehicles = vehicles;
        this.depot = depot;
        initialize();
    }

    private void initialize(){
        setNumPeriods();
        setTargetVolume();
        setDistanceMatrix();
    }

    private void setDistanceMatrix(){
        distanceMatrix = new double[customers.length][customers.length];
        for (int i = 0; i < customers.length+1; i++){
            for (int j = 0; j < customers.length+1; j++){
                if (i == customers.length+1 && j == customers.length+1){
                    distanceMatrix[i][j] = 0;
                    //todo: implement correctly
                }
                else if (i == customers.length+1 && j == customers.length+1){
                    distanceMatrix[i][j] = 0;
                    //todo: implement correctly

                }
                else{
                    distanceMatrix[i][j] = euclideanDistance(customers[i], customers[j]);

                }

            }
        }
    }

    private double euclideanDistance( Customer from_customer, Customer to_customer){
        return Math.sqrt(Math.pow( (from_customer.xCoordinate-to_customer.yCoordinate), 2 ) +
                Math.pow((from_customer.yCoordinate - to_customer.yCoordinate), 2));
    }

    private void setNumPeriods(){
        numPeriods = customers[0].timeWindow.length;
    }

    private void setTargetVolume(){
        double totalVolume = 0;
        int numDeliveries = 0;
        for (Customer c : customers){
            for (Order p : c.products){
                totalVolume += p.volume;
                numDeliveries ++;
            }
        }
        this.numDeliveries = numDeliveries;
        this.totalVolume = totalVolume;
        targetVolume = totalVolume/numPeriods;

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
