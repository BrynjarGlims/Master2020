package Master2020.PR;
import java.util.Arrays;

public class Customer {

    public int customerID;
    public double[] productQuantities;         //quantity of each product
    public int[] productTypes;              //0 for non-dividable 1 for dividable
    public double[][] timeWindow;              //interval for when customer can receive
    public int[]  maxFrequencyProduct;
    public int[]  minFrequencyProduct;
    public double[] minQuantityProduct;
    public double[] maxQuantityProduct;
    public int[] visitDays;
    public double fixedUnloadingTime;
    public double xCoordinate;
    public double yCoordinate;
    //FOR PATH GENERATION
    public double minAmountProductQuantity;
    public double distanceToDepot;
    private int currentPeriod;




    public Customer(int customerID, double[] productQuantities, int[] productTypes, double[][] timeWindow, int[] visitDays,
                    int[] minFrequencyProduct, int[] maxFrequencyProduct, double[] minQuantityProduct, double[] maxQuantityProduct,
                    double fixedUnloadingTime, double xCoordinate, double yCoordinate, double distanceToDepot){
        this.customerID = customerID;
        this.productQuantities = productQuantities;
        this.productTypes = productTypes;
        this.timeWindow = timeWindow;
        this.visitDays = visitDays;
        this.minFrequencyProduct = minFrequencyProduct;
        this.maxFrequencyProduct = maxFrequencyProduct;
        this.minQuantityProduct = minQuantityProduct;
        this.maxQuantityProduct = maxQuantityProduct;
        this.fixedUnloadingTime = fixedUnloadingTime;
        this.xCoordinate = xCoordinate;
        this.yCoordinate = yCoordinate;
        this.distanceToDepot = distanceToDepot;
        minAmountProductQuantity = getMinAmountProductQuantity();
    }

    private double getMinAmountProductQuantity(){
        double min = Double.POSITIVE_INFINITY;
        for (int i = 0 ; i < productQuantities.length ; i++) {
            if (productQuantities[i] < min && productTypes[i] == 0){
                min = productQuantities[i];
            }
        }
        return 0; // TODO: 29.04.2020 find reasonable number
    }

    public double[] getTimeWindow(int period){
        if (visitDays[period] == 0){
            return new double[]{0,0};
        }
        else{
            return timeWindow[period];
        }
    }

    public double getEndTimeWindow(){ //FOR PATH GENERATION
        return getTimeWindow(currentPeriod)[1];
    }

    public void setCurrentPeriod(int period){
        currentPeriod = period;
    }

    public String toString(){
        String str = "";
        str += "customer ID: " + customerID;
        str += "\nproduct quantities: " + Arrays.toString(productQuantities);
        str += "\nproduct types: " + Arrays.toString(productTypes);
        str += "\ntime windows: ";
        for (double[] window : timeWindow){
            str += Arrays.toString(window) + " ";
        }
        str += "\nminmum frequency: " + Arrays.toString(minFrequencyProduct);
        str += "\nmaximum frequency: " + Arrays.toString(maxFrequencyProduct);
        str += "\nvisit days: " + Arrays.toString(visitDays);
        str += "\nfixed loading time: " + fixedUnloadingTime;
        str += "\nx coordinate: " + xCoordinate;
        str += "\ny coordinate: " + yCoordinate;
        return str;
    }
}
