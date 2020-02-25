package ProductAllocation;

import DataFiles.Data;
import DataFiles.Order;

import java.util.ArrayList;

public class OrderDelivery { 


    private Order order;
    public double[] orderVolumes;
    public int[] orderPeriods;
    public int period;
    public double volume;
    public boolean dividable;


    public OrderDelivery(int numberOfPeriods, Order order, int period, double volume, boolean dividable) {
        this.dividable = dividable;
        this.order = order;
        orderVolumes = new double[numberOfPeriods];
        orderPeriods = new int[numberOfPeriods];
        orderPeriods[period] = 1;
        orderVolumes[period] = volume;
    }

    public void addDelivery(int period, double volume){
        orderVolumes[period] = volume;
        orderPeriods[period] = 1;
    }

    public void removeDeliver(int period){
        orderVolumes[period] = 0;
        orderPeriods[period] = 0;
    }



}
