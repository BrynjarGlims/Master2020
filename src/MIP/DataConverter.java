package MIP;

import DataFiles.*;
import PR.DataMIP;
import org.nustaq.offheap.bytez.bytesource.CutAsciiStringByteSource;

import java.util.HashMap;


public class DataConverter {

    static Data data;
    static DataMIP dm;
    static HashMap<VehicleType, PR.VehicleType> vehicleTypeMap = new HashMap<VehicleType, PR.VehicleType>();


    public static DataMIP convert( Data newData ){
        data = newData;
        dm = new DataMIP();

        PR.Customer[] customers = new PR.Customer[data.numberOfCustomers];
        for (int i = 0; i < data.numberOfCustomers; i++){
            customers[i] = convertCustomer(data.customers[i], data.depot);
        }

        PR.VehicleType[] vehicleTypes = new PR.VehicleType[data.numberOfVehicleTypes];
        for (int vt = 0; vt < data.numberOfVehicleTypes; vt++){
            vehicleTypes[vt] = convertVehicleType(data.vehicleTypes[vt]);
        }

        PR.Vehicle[] vehicles = new PR.Vehicle[data.numberOfVehicles];
        for (int v = 0; v < data.numberOfVehicles; v++){
            vehicles[v] = convertVehicle(data.vehicles[v]);
        }

        dm.storeMainData(customers,vehicleTypes,vehicles);
        dm.storeAdditionalData(data);

        return dm;
    }

    private static PR.Vehicle convertVehicle (Vehicle vehicle){
        return new PR.Vehicle(vehicle.vehicleID, vehicleTypeMap.get(vehicle.vehicleType));
    }



    private static PR.VehicleType convertVehicleType(VehicleType vehicleType){
        PR.VehicleType newVT = new PR.VehicleType(vehicleType.vehicleTypeID, vehicleType.travelCost, vehicleType.usageCost, vehicleType.capacity, vehicleType.loadingTimeAtDepot, data.numberOfVehiclesInVehicleType[vehicleType.vehicleTypeID]);
        vehicleTypeMap.put(vehicleType,newVT);
        return newVT;
    }

    private static PR.Customer convertCustomer(Customer customer, Depot depot){
        return new PR.Customer(customer.customerID, getProductQuantities(customer.orders), getProductTypes(customer.orders),
                customer.timeWindow, customer.requiredVisitPeriod, getMinFreq(customer.orders), getMaxFreq(customer.orders),
                getMinQuantity(customer.orders), getMaxQuantity(customer.orders), customer.totalUnloadingTime,
                customer.xCoordinate, customer.yCoordinate, distanceToDepot(customer,depot));

    }

    public static double distanceToDepot(Customer customer, Depot depot){
        return Math.sqrt(Math.pow(customer.xCoordinate - depot.xCoordinate, 2) + Math.pow(customer.yCoordinate - depot.xCoordinate, 2));

    }

    private static double[] getProductQuantities(Order[] orders){
        double[] productQuantities = new double[orders.length];
        for (int o = 0; o < orders.length; o++){
            productQuantities[o] = orders[o].volume;
        }
        return productQuantities;
    }

    private static int[] getProductTypes(Order[] orders){
        int[] productTypes = new int[orders.length];
        for (int o = 0; o < orders.length; o++){
            productTypes[o] = orders[o].isDividable ? 1 : 0;
        }
        return productTypes;
    }

    private static int[] getMinFreq(Order[] orders){
        int[] minFreq = new int[orders.length];
        for (int o = 0; o < orders.length; o++){
            minFreq[o] = orders[o].isDividable ? orders[o].minFrequency : 1;
        }
        return minFreq;
    }

    private static int[] getMaxFreq(Order[] orders){
        int[] maxFreq = new int[orders.length];
        for (int o = 0; o < orders.length; o++){
            maxFreq[o] = orders[o].isDividable ? orders[o].maxFrequency : 1;
        }
        return maxFreq;
    }

    private static double[] getMinQuantity(Order[] orders){
        double[] minQ = new double[orders.length];
        for (int o = 0; o < orders.length; o++){
            minQ[o] = orders[o].isDividable ? orders[o].minVolume : 1;
        }
        return minQ;
    }

    private static double[] getMaxQuantity(Order[] orders){
        double[] maxQ = new double[orders.length];
        for (int o = 0; o < orders.length; o++){
            maxQ[o] = orders[o].isDividable ? orders[o].maxVolume : 1;
        }
        return maxQ;
    }

    public static int[] getNumProductsPrCustomer(Customer[] customers){
        int[] productsPrCustomer = new int[customers.length];
        for (int i = 0 ; i < customers.length; i++){
            productsPrCustomer[i] = customers[i].orders.length;
        }
        return productsPrCustomer;
    }





}
