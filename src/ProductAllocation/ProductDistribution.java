package ProductAllocation;

import DataFiles.Customer;
import DataFiles.Data;
import DataFiles.DataReader;
import DataFiles.Order;

import java.util.Arrays;

public class ProductDistribution {


    public double[][] productDistribution;
    public Data data;
    public ProductDelivery[] productDeliveries;
    public double[] volumePerPeriod;

    public ProductDistribution(Data data){
        this.data = data;
        productDistribution = new double[data.numPeriods][data.customers.length];
        productDeliveries = new ProductDelivery[data.numDeliveries];
        volumePerPeriod = new double[data.numPeriods];

    }

    public void makeDistribution(Data data){
        for (Customer c : data.customers){
            int[] visitDaysCopy = new int[c.requiredVisitPeriod.length];
            System.arraycopy(c.requiredVisitPeriod, 0, visitDaysCopy, 0, c.requiredVisitPeriod.length);
            for (Order p : c.nonDividableProducts){
                System.out.println(c.customerNumber);
                int chosenDay = getMinimumPeriod(visitDaysCopy);
                productDistribution[getMinimumPeriod(c.requiredVisitPeriod)][c.customerID] += p.volume;
                visitDaysCopy[chosenDay] = 0;
            }
        }
    }


    private int getMinimumPeriod(int[] possibleDays){
        int currentIndex = -1;
        for (int i = 0 ; i < possibleDays.length ; i++){
            if (possibleDays[i] == 1){
                currentIndex = i;
                break;
            }
        }
        System.out.println(Arrays.toString(possibleDays));
        if (currentIndex == -1){throw new IllegalStateException("customer has no valid visit days");}

        for (int i = 0 ; i < volumePerPeriod.length ; i++){
            if (possibleDays[i] == 0) {continue;}
            if (volumePerPeriod[i] < volumePerPeriod[currentIndex]){
                    currentIndex = i;
            }
        }
        return currentIndex;
    }


    public static void main(String[] args){
        Data data = DataReader.loadData();

        ProductDistribution pd = new ProductDistribution(data);
        pd.makeDistribution(data);
        for(double[] period : pd.productDistribution){
            double sum = 0;
            for(double d : period){
                sum += d;
            }
            System.out.println(sum);
        }
    }


}
