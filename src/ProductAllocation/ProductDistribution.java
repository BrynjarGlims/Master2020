package ProductAllocation;

import DataFiles.Customer;
import DataFiles.Data;
import DataFiles.DataReader;
import DataFiles.Order;
import javafx.scene.shape.ArcTo;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ProductDistribution {


    public double[][] productDistribution;
    public Data data;
    public OrderDelivery[] orderDeliveries;
    public double[] volumePerPeriod;

    public ProductDistribution(Data data){
        this.data = data;
        productDistribution = new double[data.numPeriods][data.customers.length];
        orderDeliveries = new OrderDelivery[data.numDeliveries];
        volumePerPeriod = new double[data.numPeriods];

    }

    public void makeDistribution(){
        distributeDividables();
    }


    private void distributeNonDividables(){
        for (Customer c : data.customers){
            int[] visitDaysCopy = new int[c.requiredVisitPeriod.length];
            System.arraycopy(c.requiredVisitPeriod, 0, visitDaysCopy, 0, c.requiredVisitPeriod.length);
            for (Order o : c.nonDividableProducts){
                int chosenPeriod = getMinimumPeriod(visitDaysCopy);
                productDistribution[chosenPeriod][c.customerID] += o.volume;
                volumePerPeriod[chosenPeriod] += o.volume;
                visitDaysCopy[chosenPeriod] = 0;

                orderDeliveries[o.orderID] = new OrderDelivery(o, chosenPeriod, o.volume);
            }
        }
    }

    private void distributeDividables(){
        for (Customer c : data.customers){
            for (Order o : c.dividableProducts){
            }
        }


    }

    private double[] splitDelivery(Order order){
        int numSplits = ThreadLocalRandom.current().nextInt(order.minFrequency, order.maxFrequency + 1);
        double[] volumeSplits = distributeVolume(order,numSplits, 10);


        return volumeSplits;
    }

    private double[] distributeVolume(Order order, int numSplits, int numFractions){
        int fractions = numSplits*numFractions;
        Queue<Integer> splits = new ArrayDeque<>();
        double sum = 0;
        int randomNumber;
        for (int i = 0 ; i < fractions ; i++){
            randomNumber = ThreadLocalRandom.current().nextInt(10,100);
            sum += randomNumber;
            splits.add(randomNumber);
        }
        double[] distribution = new double[numSplits];
        int tempFraction;
        for (int i = 0 ; i < numSplits ; i++){
            tempFraction = 0;
            for (int j = 0 ; j < numFractions ; j++){
                tempFraction += splits.remove();
            }
            distribution[i] = order.volume * (tempFraction/sum);
        }
    return distribution;
    }

    private int[] getValidTimeWindows(double[] volumes, Customer customer){
        ArrayList<Integer> periods = new ArrayList<>();
        for (int i = 0 ; i < customer.numberOfVisitPeriods ; i++) {
            if (customer.requiredVisitPeriod[i] == 1) {
                periods.add(i);
            }
        }
        Collections.shuffle(periods);
        int[] orderDeliveries = new int[volumes.length];
        for (int i = 0 ; i < volumes.length ; i++){
            orderDeliveries[i] = periods.get(i);
            }
        return orderDeliveries;
        }



//        int[] deliveryPeriods = new int[data.numPeriods];
//        int[] visitDaysCopy = new int[customer.requiredVisitPeriod.length];
//        System.arraycopy(customer.requiredVisitPeriod, 0, visitDaysCopy, 0, customer.requiredVisitPeriod.length);
//        if (volumes.length == 1){
//            deliveryPeriods[getMinimumPeriod(visitDaysCopy)] = 1;
//        }
//        else if (volumes.length == 2){
//
//        }
//
//
//
//        return new int[2];
    }

    private int getMinimumPeriod(int[] possibleDays){
        List<Integer> validPeriods = new ArrayList<>();
        for (int i = 0 ; i < possibleDays.length ; i++){
            if (possibleDays[i] == 1){
                validPeriods.add(i);
            }
        }
        if (validPeriods.isEmpty()){throw new IllegalStateException("customer has no valid visit days");}
        int currentIndex = validPeriods.get(ThreadLocalRandom.current().nextInt(validPeriods.size()));


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
        pd.makeDistribution();
        for(double[] period : pd.productDistribution){
            double sum = 0;
            for(double customer : period){
                sum += customer;
            }
        }
        double[] a = pd.distributeVolume(data.customers[2].dividableProducts[0],
                ThreadLocalRandom.current().nextInt(data.customers[2].dividableProducts[0].minFrequency, data.customers[2].dividableProducts[0].maxFrequency + 1), 3);
        System.out.println(Arrays.toString(a));
        double sum = 0;
        for (double e : a){
            sum += e;
        }
        System.out.println(sum);
        System.out.println(data.customers[2].dividableProducts[0].volume);


    }



}
