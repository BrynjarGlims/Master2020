import java.util.List;

public class Customer {
    //DL = detaljerte leveringstider
    //FFV = Frajsoner, frekvenser og volum


    public int customerID; //customer code, DL
    public String customerName; // kundenavn
    public Product[] products;
    public double[][] timeWindow;              // interval for when customer can receive, day, product
    public int[] visitDays;

    public double xCoordinate;
    public double yCoordinate;

    public double fixedLoadingTime;
    public double variableLoadingTime;
    public double fixedUnloadingTime;
    public double variableUnloadingTime;


    public Customer(int customerID, String customerName){
        this.customerID = customerID;
        this.customerName = customerName;
    }

    public void setProducts( Product[] products){
        this.products = products;
    }

    public void setTimeWindow(double[][] timeWindow){
        this.timeWindow = timeWindow;
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
