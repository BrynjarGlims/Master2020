package Testing;

import DataFiles.Customer;
import DataFiles.Data;
import Individual.Individual;

import java.util.HashMap;
import java.util.HashSet;

public class IndividualTest {
    public static void checkIfIndividualIsComplete( Individual individual) throws Exception {
        for (int p = 0; p < individual.data.numberOfPeriods; p++){
            for (int i = 0; i < individual.data.numberOfCustomers; i++){
                if (individual.tripMap.get(p).containsKey(i)){
                    boolean customerFound = false;
                    for (int vt = 0; vt < individual.data.numberOfVehicleTypes; vt++){
                        if (individual.giantTour.chromosome[p][vt].contains(i)){
                            customerFound = true;
                        }
                    }
                    if (!customerFound){
                        System.out.println("Cannot find trip for period "+p+ " customer " +i );
                        throw new Exception();
                    }
                }
            }
        }
    }


    public static boolean testTripMap(Individual individual){
        Data data = individual.data;
        for (int p = 0 ; p < data.numberOfPeriods ; p++){
            for (Customer customer : data.customers){
                if (customer.requiredVisitPeriod[p] == 1){
                    if (!individual.tripMap.get(p).containsKey(customer.customerID)){
                        System.out.println("missing customer: " + customer.customerID + " in period " + p);
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static void isMissingCustomersAdded( HashMap<Integer, HashSet<Integer>> missingCustomers, Individual individual) {
        for ( int p : missingCustomers.keySet()){
            for (int i : missingCustomers.get(p)){
                boolean foundCustomer = false;
                for (int vt = 0; vt < individual.data.numberOfVehicleTypes; vt ++){
                    if (individual.giantTour.chromosome[p][vt].contains(i)){
                        foundCustomer = true;
                        break;
                    }
                }
                if (!foundCustomer){
                    System.out.println("Cannot find trip for period "+p+ " customer " +i );
                    try {
                        throw new Exception();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        }
    }

}
