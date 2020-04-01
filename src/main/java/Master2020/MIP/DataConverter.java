package Master2020.MIP;

import Master2020.DataFiles.*;
import Master2020.PR.DataMIP;
import org.nustaq.offheap.bytez.bytesource.CutAsciiStringByteSource;

import java.util.HashMap;


public class DataConverter {

    static Data data;
    static DataMIP dm;
    static HashMap<VehicleType, Master2020.PR.VehicleType> vehicleTypeMap = new HashMap<VehicleType, Master2020.PR.VehicleType>();


    public static DataMIP convert( Data newData ){
        data = newData;
        dm = new DataMIP();
        dm.setNewData(data);

        Master2020.PR.Customer[] customers = new Master2020.PR.Customer[data.numberOfCustomers];
        for (int i = 0; i < data.numberOfCustomers; i++){
            customers[i] = convertCustomer(data.customers[i], data.depot);
        }

        Master2020.PR.VehicleType[] vehicleTypes = new Master2020.PR.VehicleType[data.numberOfVehicleTypes];
        for (int vt = 0; vt < data.numberOfVehicleTypes; vt++){
            vehicleTypes[vt] = convertVehicleType(data.vehicleTypes[vt]);
        }

        Master2020.PR.Vehicle[] vehicles = new Master2020.PR.Vehicle[data.numberOfVehicles];
        for (int v = 0; v < data.numberOfVehicles; v++){
            vehicles[v] = convertVehicle(data.vehicles[v]);
        }
        setVehiclesInVehicleTypes(vehicles, vehicleTypes);

        dm.storeMainData(customers,vehicleTypes,vehicles);
        dm.storeAdditionalData(data);

        return dm;
    }

    private static void setVehiclesInVehicleTypes(Master2020.PR.Vehicle[] vehicles, Master2020.PR.VehicleType[] vehicleTypes){
        for (Master2020.PR.VehicleType vt : vehicleTypes){
            int counter = 0;
            for (Master2020.PR.Vehicle v : vehicles){
                if (v.vehicleType.type == vt.type){
                    vt.addVehicle(v, counter);
                    counter++;
                }
            }
        }
    }

    private static Master2020.PR.Vehicle convertVehicle (Vehicle vehicle){
        return new Master2020.PR.Vehicle(vehicle.vehicleID, vehicleTypeMap.get(vehicle.vehicleType));
    }



    private static Master2020.PR.VehicleType convertVehicleType(VehicleType vehicleType){
        Master2020.PR.VehicleType newVT = new Master2020.PR.VehicleType(vehicleType.vehicleTypeID, vehicleType.travelCost, vehicleType.usageCost, vehicleType.capacity, vehicleType.loadingTimeAtDepot, data.numberOfVehiclesInVehicleType[vehicleType.vehicleTypeID]);
        vehicleTypeMap.put(vehicleType,newVT);
        return newVT;
    }

    private static Master2020.PR.Customer convertCustomer(Customer customer, Depot depot){
        return new Master2020.PR.Customer(customer.customerID, getProductQuantities(customer.orders), getProductTypes(customer.orders),
                customer.timeWindow, customer.requiredVisitPeriod, getMinFreq(customer.orders), getMaxFreq(customer.orders),
                getMinQuantity(customer.orders), getMaxQuantity(customer.orders), customer.totalUnloadingTime,
                customer.xCoordinate, customer.yCoordinate, distanceToDepot(customer,depot));

    }

    public static double distanceToDepot(Customer customer, Depot depot){
        return Math.sqrt(Math.pow(customer.xCoordinate - depot.xCoordinate, 2) + Math.pow(customer.yCoordinate - depot.yCoordinate, 2))* Parameters.scalingDistanceParameter;

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
