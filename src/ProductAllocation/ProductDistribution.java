package ProductAllocation;

import DataFiles.Customer;
import DataFiles.Data;
import DataFiles.DataReader;
import DataFiles.Order;

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
        distributeNonDividables();
    }


    private void distributeNonDividables(){
        for (Customer c : data.customers){
            int[] visitDaysCopy = new int[c.requiredVisitPeriod.length];
            System.arraycopy(c.requiredVisitPeriod, 0, visitDaysCopy, 0, c.requiredVisitPeriod.length);
            for (Order o : c.nonDividableProducts){
                int chosenPeriod = getMinimumPeriod(visitDaysCopy);
                updateFields(o, chosenPeriod, o.volume);
                visitDaysCopy[chosenPeriod] = 0;
            }
        }
    }

    private void distributeDividables(){
        for (Customer c : data.customers){
            for (Order o : c.dividableProducts){
                splitDelivery(o);
            }
        }
    }

    private double[] splitDelivery(Order order){
        int numSplits = ThreadLocalRandom.current().nextInt(order.minFrequency, order.maxFrequency + 1);
        double[] volumeSplits = distributeVolume(order,numSplits, 10); //numFractions decide amount of randomness in distribution
        int[] deliveryPeriods = getValidDeliveryPeriods(volumeSplits, data.customers[order.customerID]);

        for (int i = 0 ; i < deliveryPeriods.length ; i++){
            updateFields(order, deliveryPeriods[i], volumeSplits[i]);
        }
        return volumeSplits;
    }

    private double[] distributeVolume(Order order, int numSplits, int numFractions){
        int fractions = numSplits*numFractions;
        Queue<Integer> splits = new ArrayDeque<>();
        double sum = 0;
        int randomNumber;
        for (int i = 0 ; i < fractions ; i++){
            randomNumber = ThreadLocalRandom.current().nextInt(10,100); //for more randomness, set bigger interval
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

    private int[] getValidDeliveryPeriods(double[] volumes, Customer customer){ // TODO: 07.02.2020 no validity is applied to construction
        ArrayList<Integer> periods = new ArrayList<>();
        for (int i = 0 ; i < data.numPeriods ; i++) {
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

    private void updateFields(Order order, int period, double volume){
        productDistribution[period][order.customerID] += volume;
        volumePerPeriod[period] += volume;
        orderDeliveries[order.orderID] = new OrderDelivery(order, period, volume);
    }


    public static void main(String[] args){
        Data data = DataReader.loadData();
        ProductDistribution pd = new ProductDistribution(data);
        pd.makeDistribution();

        for (double[] period : pd.productDistribution){
            System.out.println(Arrays.toString(period));
        }
    }
}
