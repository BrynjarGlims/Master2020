package DataFiles;

import java.util.ArrayList;
import java.util.List;

public class Customer {
    //DL = detaljerte leveringstider
    //FFV = Frajsoner, frekvenser og volum


    public int customerID;
    public int customerNumber; //customer code, DL
    public String customerName; // kundenavn
    public Order[] orders;
    public Order[] dividableProducts;
    public Order[] nonDividableProducts;
    public int numberOfDividableProducts = 0;
    public int numberOfNonDividableProducts = 0;
    public double[][] timeWindow;// interval for when customer can receive, [day][start, end]
    public int[] requiredVisitPeriod;    // 1 if visit required, 0 if not
    public int numberOfVisitPeriods;
    public int numberOfOrders;

    public double xCoordinate;
    public double yCoordinate;

    public double fixedLoadingTime;
    public double variableLoadingTime;
    public double fixedUnloadingTime;
    public double variableUnloadingTime;


    public Customer(int customerID, int customerNumber, String customerName){
        this.customerID = customerID;
        this.customerNumber = customerNumber;
        this.customerName = customerName;
    }

    public void setCustomerID(int customerID){

        this.customerID = customerID;
        for(int i = 0; i < orders.length; i++){
            this.orders[i].customerID = this.customerID;
        }

    }

    public void setOrders(Order[] orders){
        this.orders = orders;
        this.numberOfOrders = orders.length;
        setDividableAndNonDividable();
    }

    private void setDividableAndNonDividable(){
        List<Order> dividable = new ArrayList<>();
        List<Order> nonDividable = new ArrayList<>();
        for (Order p : orders){
            if (p.isDividable) {
                dividable.add(p);
                this.numberOfDividableProducts++;

            } else {
                nonDividable.add(p);
                this.numberOfNonDividableProducts++;
            }
        }
        dividableProducts = dividable.toArray(Order[]::new);
        nonDividableProducts = nonDividable.toArray(Order[]::new);
    }

    public void setTimeWindow(double[][] timeWindow){
        this.timeWindow = timeWindow;
        this.requiredVisitPeriod = new int[timeWindow.length];
        this.numberOfVisitPeriods = 0;
        for (int i = 0; i < timeWindow.length; i++){
            if (timeWindow[i][1] > 0.0){
                this.requiredVisitPeriod[i] = 1;
                this.numberOfVisitPeriods += 1;
            }
            else{
                this.requiredVisitPeriod[i] = 0;
            }
        }
    }

    public void setCoordinates(double[] coordinates){
        this.xCoordinate = coordinates[0];
        this.yCoordinate = coordinates[1];
    }

    public void setLoadingTimes(double[] loadingTimes){
        this.fixedLoadingTime = loadingTimes[0];
        this.variableLoadingTime = loadingTimes[1];
        this.fixedUnloadingTime = loadingTimes[2];
        this.variableUnloadingTime = loadingTimes[3];
    }

    public String toString(){
        return "";
    }






}
