package Master2020.ProductAllocation;

import Master2020.DataFiles.Customer;
import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.DataFiles.Order;
import Master2020.DataFiles.*;
import Master2020.MIP.OrderAllocationModel;
import Master2020.PR.DataMIP;
import Master2020.PR.JourneyBasedModel;
import Master2020.PR.PathFlowModel;
import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBVar;


import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.DoubleStream;

public class OrderDistribution {


    public double[][] orderVolumeDistribution;  //period, customer
    public ArrayList<Integer>[][] orderIdDistribution;   //period, customer -- orderids, tilsvarende order volumes
    public Data data;
    public OrderDelivery[] orderDeliveries;  //orderids, slår opp på orderIdDistribution verdien
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


    public void makeDistributionFromOrderAllocationModel(OrderAllocationModel oam ) throws GRBException {
        setVolumeAndOrdersFromMIP( oam.uND, oam.uD, oam.qND, oam.qD);
        setVolumePerPeriod();
        setFitness();
    }

    public void makeDistributionFromArcFlowModel(ArcFlowModel afm) throws GRBException {
        setVolumeAndOrdersFromMIP( afm.uND, afm.uD, afm.qND, afm.qD);
        setVolumePerPeriod();
        setFitness();
    }

    public void makeDistributionFromArcFlowModel(Master2020.PR.ArcFlowModel afm) throws GRBException {
        setVolumeAndOrdersFromMIP( afm.u, afm.q, afm.dataMIP);
        setVolumePerPeriod();
        setFitness();
    }

    public void makeDistributionFromJourneyBasedModel(JourneyBasedModel jbm) throws GRBException {
        setVolumeAndOrdersFromMIP( jbm.u, jbm.q, jbm.dataMIP);
        setVolumePerPeriod();
        setFitness();
    }

    public void makeDistributionFromPathFlowModel(PathFlowModel pfm) throws GRBException {
        setVolumeAndOrdersFromMIP( pfm.u, pfm.q, pfm.dataMIP);
        setVolumePerPeriod();
        setFitness();
    }

    private void setVolumeAndOrdersFromMIP(GRBVar[][][] u, GRBVar[][][][][] q, DataMIP dataMIP) throws GRBException {
        int orderID;
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int i = 0; i < data.numberOfCustomers; i++) {
                for (int m = 0; m < data.customers[i].orders.length; m++) {
                    if (Math.round(u[d][i][m].get(GRB.DoubleAttr.X)) == 1) {
                        for (int v = 0; v < data.numberOfVehicles; v++) {
                            for (int r = 0; r < data.numberOfTrips; r++) {
                                if (q[d][v][r][i][m].get(GRB.DoubleAttr.X) > Parameters.indifferenceValue){
                                    orderID = data.customers[i].orders[m].orderID;
                                    this.orderIdDistribution[d][i].add(orderID);
                                    this.orderDeliveries[orderID].addDelivery(d, q[d][v][r][i][m].get(GRB.DoubleAttr.X));
                                    this.orderVolumeDistribution[d][i] += q[d][v][r][i][m].get(GRB.DoubleAttr.X);
                                }
                            }
                        }
                    }
                }
            }
        }
    }



    private void setVolumeAndOrdersFromMIP(GRBVar[][][] uND, GRBVar[][][] uD, GRBVar[][][][][] qND, GRBVar[][][][][] qD ) throws GRBException {
        int orderID;
        for (int d = 0; d < data.numberOfPeriods; d++){
            for (int i = 0; i < data.numberOfCustomers; i++) {
                for (int m = 0; m < data.customers[i].numberOfDividableOrders; m++){
                    if (Math.round(uD[d][i][m].get(GRB.DoubleAttr.X)) == 1) {
                        for (int v = 0; v < data.numberOfVehicles; v++){
                            for (int r = 0; r < data.numberOfTrips; r++) {
                                if (qD[d][v][r][i][m].get(GRB.DoubleAttr.X) > Parameters.indifferenceValue) {
                                    orderID = data.customers[i].dividableOrders[m].orderID;
                                    this.orderIdDistribution[d][i].add(orderID);
                                    this.orderDeliveries[orderID].addDelivery(d, qD[d][v][r][i][m].get(GRB.DoubleAttr.X));
                                    this.orderVolumeDistribution[d][i] += qD[d][v][r][i][m].get(GRB.DoubleAttr.X);
                                }
                            }
                        }

                    }
                }
                for (int m = 0; m < data.customers[i].numberOfNonDividableOrders; m++){
                    if (Math.round(uND[d][i][m].get(GRB.DoubleAttr.X)) == 1) {
                        for (int v = 0; v < data.numberOfVehicles; v++) {
                            for (int r = 0; r < data.numberOfTrips; r++) {
                                if (qND[d][v][r][i][m].get(GRB.DoubleAttr.X) > Parameters.indifferenceValue) {
                                    orderID = data.customers[i].nonDividableOrders[m].orderID;
                                    this.orderIdDistribution[d][i].add(orderID);
                                    this.orderDeliveries[orderID].addDelivery(d, qND[d][v][r][i][m].get(GRB.DoubleAttr.X));
                                    this.orderVolumeDistribution[d][i] += qND[d][v][r][i][m].get(GRB.DoubleAttr.X);
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    private void setVolumeAndOrdersFromMIP(GRBVar[][][] uND, GRBVar[][][] uD, GRBVar[][][] qND, GRBVar[][][] qD ) throws GRBException {
        for (int d = 0; d < data.numberOfPeriods; d++){
            for (int i = 0; i < data.numberOfCustomers; i++) {
                for (int m = 0; m < data.customers[i].numberOfDividableOrders; m++){
                    if (Math.round(uD[d][i][m].get(GRB.DoubleAttr.X)) == 1) {
                        this.orderIdDistribution[d][i].add(data.customers[i].dividableOrders[m].orderID);
                        this.orderDeliveries[data.customers[i].dividableOrders[m].orderID].addDelivery(d, qD[d][i][m].get(GRB.DoubleAttr.X));
                        this.orderVolumeDistribution[d][i] += qD[d][i][m].get(GRB.DoubleAttr.X);
                    }
                }
                for (int m = 0; m < data.customers[i].numberOfNonDividableOrders; m++){
                    if (Math.round(uND[d][i][m].get(GRB.DoubleAttr.X)) == 1) {
                        this.orderIdDistribution[d][i].add(data.customers[i].nonDividableOrders[m].orderID);
                        this.orderDeliveries[data.customers[i].nonDividableOrders[m].orderID].addDelivery(d, qND[d][i][m].get(GRB.DoubleAttr.X));
                        this.orderVolumeDistribution[d][i] += qND[d][i][m].get(GRB.DoubleAttr.X);
                    }
                }
            }
        }
    }

    private void setFitness(){
        fitness = 0;
        for (int p = 0; p < data.numberOfPeriods; p++){
            fitness += Parameters.overtimeCost[p]*Math.max(0, this.volumePerPeriod[p] - Data.overtimeLimit[p]);
        }
    }

    public double getFitness(){
        fitness = 0;
        for (int p = 0; p < data.numberOfPeriods; p++){
            fitness += Parameters.overtimeCost[p]*Math.max(0, this.volumePerPeriod[p] - Data.overtimeLimit[p]);
        }
        return fitness;
    }



    private void setVolumePerPeriod(){
        for (int p = 0; p < data.numberOfPeriods; p++){
            this.volumePerPeriod[p] = DoubleStream.of(this.orderVolumeDistribution[p]).sum();
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
            fitness += Parameters.overtimeCost[d]*Math.max(0 , this.volumePerPeriod[d]-Data.overtimeLimit[d]);
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
            //System.out.println(Arrays.toString(period));
        }
        for (ArrayList<Integer>[] period : pd.orderIdDistribution){
            for (ArrayList<Integer> customer : period){
                //System.out.println(customer);
                for (int i : customer){
                    //System.out.println(pd.orderDeliveries[i]);
                }
            }
        }
    }
}

