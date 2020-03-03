package ProductAllocation;

import DataFiles.Customer;
import DataFiles.Data;
import DataFiles.DataReader;
import DataFiles.Order;
import DataFiles.*;
import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBVar;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class OrderDistribution {


    public double[][] orderVolumeDistribution;
    public ArrayList<Integer>[][] orderIdDistribution;
    public Data data;
    public OrderDelivery[] orderDeliveries;  //orderids
    public double[] volumePerPeriod;

    // fitness values
    public double fitness = Double.MAX_VALUE;


    public OrderDistribution(Data data) {
        this.data = data;
        orderVolumeDistribution = new double[data.numberOfPeriods][data.customers.length];
        orderIdDistribution = new ArrayList[data.numberOfPeriods][data.customers.length];
        for (int row = 0 ; row < data.numberOfPeriods ; row++){
            for (int col = 0 ; col < data.customers.length ; col++){
                orderIdDistribution[row][col] = new ArrayList<>();
            }
        }
        orderDeliveries = new OrderDelivery[data.numberOfDeliveries];
        for (int i = 0 ; i < data.numberOfDeliveries ; i++){
            orderDeliveries[i] = new OrderDelivery(data.numberOfPeriods, data.orders[i]);
        }
        volumePerPeriod = new double[data.numberOfPeriods];
    }


    public void makeInitialDistribution() {
        distributeDividables();
        distributeNonDividables();
    }


    public void makeDistributionFromMIP(GRBVar[][][] uND, GRBVar[][][] uD, GRBVar[][][] qND, GRBVar[][][] qD, GRBVar[] qO){
        setDivOrdersFromMIP( uD, qD);
        setNonDivOrdersFromMIP(uND, qND);
        setOvertimeAtDepot(qO);
    }

    private void setOvertimeAtDepot(GRBVar[] qO){


    }

    private void setDivOrdersFromMIP(GRBVar[][][] uD, GRBVar[][][] qD ) throws GRBException {
        for (int d = 0; d < data.numberOfPeriods; d++){
            for (int i = 0; i < data.numberOfCustomers; i++){
                for (int m = 0; m < data.customers[i].numberOfDividableOrders; m++)
                    if (uD[d][i][m].get(GRB.DoubleAttr.X) == 1){
                        this.orderIdDistribution[d][i].add(data.customers[i].dividableOrders[m].orderID);
                        this.orderDeliveries[data.customers[i].dividableOrders[m].orderID].addDelivery(d, qD[d][i][m].get);
                    }
                for (Order order : data.customers[i].dividableOrders)
            }
        }
    }

    private void setNonDivOrdersFromMIP(GRBVar[][][] uND, GRBVar[][][] qND ) {
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int i = 0; i < data.numberOfCustomers; i++) {
                for (Order order : data.customers[i].dividableOrders)
            }
        }
    }




    public void addOrderDelivery(OrderDelivery orderDelivery){
        for (int i = 0 ; i < orderDelivery.orderPeriods.length ; i++){
            if (orderDelivery.orderPeriods[i] == 1){
                addOrderDelivery(i, orderDelivery.orderVolumes[i], orderDelivery.order);
            }
        }
    }

    public boolean containsNonDividable(int customer, int period){
        for (int i : orderIdDistribution[period][customer]){
            if (!data.orders[i].isDividable){
                return true;
            }
        }
        return false;
    }

    public int getBestPeriod(int order){
        int[] visitDaysCopy = new int[data.customers[data.orders[order].customerID].requiredVisitPeriod.length];
        System.arraycopy(data.customers[data.orders[order].customerID].requiredVisitPeriod, 0, visitDaysCopy, 0, data.customers[data.orders[order].customerID].requiredVisitPeriod.length);
        int currentBest;
        while (true){
            currentBest = getMinimumPeriod(visitDaysCopy);
            if (!containsNonDividable(data.orders[order].customerID, currentBest)){
                return currentBest;
            }
            else{
                visitDaysCopy[currentBest] = 0;

            }
        }
    }


    private void distributeNonDividables() {
        int chosenPeriod;
        for (Customer c : data.customers) {
            int[] visitDaysCopy = new int[c.requiredVisitPeriod.length];
            System.arraycopy(c.requiredVisitPeriod, 0, visitDaysCopy, 0, c.requiredVisitPeriod.length);
            for (Order o : c.nonDividableOrders) {
                chosenPeriod = getMinimumPeriod(visitDaysCopy);
                updateFields(o, chosenPeriod, o.volume);
                visitDaysCopy[chosenPeriod] = 0;
            }
        }
    }

    private void distributeDividables() {
        for (Customer c : data.customers) {
            for (Order o : c.dividableOrders) {
                splitDelivery(o);
            }
        }
    }

    private double[] splitDelivery(Order order) {
        int numSplits = ThreadLocalRandom.current().nextInt(order.minFrequency, order.maxFrequency + 1);
        double[] volumeSplits = distributeVolume(order, numSplits, 10); //numFractions decide amount of randomness in distribution
        int[] deliveryPeriods = getValidDeliveryPeriods(volumeSplits, data.customers[order.customerID]);

        for (int i = 0; i < deliveryPeriods.length; i++) {
            updateFields(order, deliveryPeriods[i], volumeSplits[i]);
        }
        return volumeSplits;
    }

    private double[] distributeVolume(Order order, int numSplits, int numFractions) {
        int fractions = numSplits * numFractions;
        Queue<Integer> splits = new ArrayDeque<>();
        double sum = 0;
        int randomNumber;
        for (int i = 0; i < fractions; i++) {
            randomNumber = ThreadLocalRandom.current().nextInt(10, 100); //for more randomness, set bigger interval
            sum += randomNumber;
            splits.add(randomNumber);
        }
        double[] distribution = new double[numSplits];
        int tempFraction;
        for (int i = 0; i < numSplits; i++) {
            tempFraction = 0;
            for (int j = 0; j < numFractions; j++) {
                tempFraction += splits.remove();
            }
            distribution[i] = order.volume * (tempFraction / sum);
        }
        return distribution;
    }

    private int[] getValidDeliveryPeriods(double[] volumes, Customer customer) { // TODO: 07.02.2020 no validity is applied to construction
        ArrayList<Integer> periods = new ArrayList<>();
        for (int i = 0; i < data.numberOfPeriods; i++) {
            if (customer.requiredVisitPeriod[i] == 1) {
                periods.add(i);
            }
        }
        Collections.shuffle(periods);

        int[] orderDeliveries = new int[volumes.length];
        for (int i = 0; i < volumes.length; i++) {
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

    private void addOrderDelivery(int period, double volume, Order order){
        orderVolumeDistribution[period][order.customerID] += volume;
        orderIdDistribution[period][order.customerID].add(order.orderID);
        orderDeliveries[order.orderID].addDelivery(period, volume);
        volumePerPeriod[period] += volume;
    }


    private int getMinimumPeriod(int[] possibleDays) {
        List<Integer> validPeriods = new ArrayList<>();
        for (int i = 0; i < possibleDays.length; i++) {
            if (possibleDays[i] == 1) {
                validPeriods.add(i);
            }
        }
        if (validPeriods.isEmpty()) {
            throw new IllegalStateException("customer has no valid visit days");
        }
        int currentIndex = validPeriods.get(ThreadLocalRandom.current().nextInt(validPeriods.size()));


        for (int i = 0; i < volumePerPeriod.length; i++) {
            if (possibleDays[i] == 0) {
                continue;
            }
            if (volumePerPeriod[i] < volumePerPeriod[currentIndex]) {
                currentIndex = i;
            }
        }
        return currentIndex;
    }

    private void updateFields(Order order, int period, double volume) {
        orderVolumeDistribution[period][order.customerID] += volume;
        orderIdDistribution[period][order.customerID].add(order.orderID);
        volumePerPeriod[period] += volume;
        orderDeliveries[order.orderID].addDelivery(period, volume);
    }

    public double getOvertimeValue(){
        fitness = 0;
        for (int d = 0; d < data.numberOfPeriods; d++ ){
            fitness += Parameters.overtimeCost[d]*Math.max(0 , Arrays.stream(this.orderVolumeDistribution[d]).sum()-Parameters.overtimeLimit[d]);
        }
        return fitness;
    }

    public String toString(){
        String out = "";
        for (OrderDelivery od : orderDeliveries){
            if (od != null){

                out += od.toString();
            }
        }
        return out;
    }


    public static void main(String[] args) {
        Data data = DataReader.loadData();
        OrderDistribution pd = new OrderDistribution(data);
        pd.makeInitialDistribution();
        for (double[] period : pd.orderVolumeDistribution) {
            System.out.println(Arrays.toString(period));
        }
        for (ArrayList<Integer>[] period : pd.orderIdDistribution){
            for (ArrayList<Integer> customer : period){
                System.out.println(customer);
                for (int i : customer){
                    System.out.println(pd.orderDeliveries[i]);
                }
            }
        }
    }
}

