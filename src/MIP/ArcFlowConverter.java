package MIP;

import DataFiles.Data;
import Individual.Individual;
import ProductAllocation.OrderDistribution;
import gurobi.GRB;
import gurobi.GRBException;
import Individual.Trip;
import Individual.GiantTour;


import java.util.ArrayList;
import java.util.HashMap;

public class ArcFlowConverter {

    private static OrderDistribution orderDistribution;
    private static Individual individual;
    private static ArcFlowModel arcFlowModel;
    private static Data data;


    public static void initializeIndividualFromArcFlowModel(ArcFlowModel afm) throws GRBException {
        arcFlowModel = afm;
        data = arcFlowModel.data;
        orderDistribution = new OrderDistribution(data);
        orderDistribution.makeDistributionFromArcFlowModel(arcFlowModel);
        individual = new Individual(data);
        //set giant tour chromosome and support strucutres in new individual
        initializeGiantTourInIndividual(arcFlowModel);

    }





    private static void initializeGiantTourInIndividual(ArcFlowModel afm) throws GRBException {

        ArrayList<Trip>[][] tripList = setTripList();
        updateTripList(tripList);
        ArrayList<Integer>[][] giantTour = initializeGiantTourChromosome();
        updateGiantTourChromosome(giantTour,tripList);
        HashMap<Integer, HashMap<Integer,Trip>> tripMap = getTripMap(tripList);
        individual.setTripMap(tripMap);
        individual.setTripList(tripList);
        individual.setGiantTour(createGiantTour(giantTour));

    }

    private static GiantTour createGiantTour( ArrayList<Integer>[][] giantTour){
        GiantTour gt = new GiantTour(data);
        gt.setChromosome(giantTour);
        return gt;
    }

    private static HashMap<Integer, HashMap<Integer,Trip>> getTripMap(ArrayList<Trip>[][] tripList) {
        HashMap<Integer, HashMap<Integer,Trip>> tripMap = new HashMap<Integer, HashMap<Integer,Trip>>();
        for (int p = 0; p < data.numberOfPeriods; p++){
            tripMap.put(p, new HashMap<Integer, Trip>());
            for (int vt = 0; vt < data.numberOfVehicleTypes; vt ++){
                for (Trip trip : tripList[p][vt]){
                    for (int customer : trip.getCustomers()){
                        tripMap.get(p).put(customer, trip);
                    }
                }
            }
        }
        return tripMap;
    }

    private static ArrayList<Integer>[][] initializeGiantTourChromosome(){
        ArrayList<Integer>[][] giantTour = new ArrayList[data.numberOfPeriods][data.numberOfVehicleTypes];
        for (int p = 0; p < data.numberOfPeriods; p++){
            for (int vt = 0; vt < data.numberOfVehicleTypes; vt++){
                giantTour[p][vt] = new ArrayList<>();
            }
        }
        return giantTour;
    }

    private static void updateGiantTourChromosome(ArrayList<Integer>[][] giantTour, ArrayList<Trip>[][] tripList){
        for (int p = 0; p < data.numberOfPeriods; p++) {
            for (int vt = 0; vt < data.numberOfVehicleTypes; vt++) {
                for (Trip trip : tripList[p][vt]) {
                    for (int customer : trip.getCustomers()) {
                        giantTour[p][vt].add(customer);
                    }
                }
            }
        }
    }



    private static void updateTripList(ArrayList<Trip>[][] tripList) throws GRBException {
        Trip currentTrip;
        int fromCustomer;
        ArrayList<Integer> customers;

        for ( int p = 0; p < data.numberOfPeriods; p ++ ){
            for (int v = 0; v < data.numberOfVehicles; v ++){
                for (int r = 0; r < data.numberOfTrips; r++){
                    if (arcFlowModel.z[p][v][r].get(GRB.DoubleAttr.X) == 1){
                        for( int i = 0; i < data.numberOfCustomers; i++){
                            if (arcFlowModel.x[p][v][r][data.numberOfCustomers][i].get(GRB.DoubleAttr.X) == 1){
                                currentTrip = new Trip(data);
                                customers = new ArrayList<>(i);
                                currentTrip.initialize(p, data.vehicles[v].vehicleType.vehicleTypeID, v);
                                fromCustomer = i;
                                while (arcFlowModel.x[p][v][r][fromCustomer][data.numberOfCustomers+1].get(GRB.DoubleAttr.X) == 0){
                                    for (int j = 0; j < data.numberOfCustomers; j++){
                                        if( arcFlowModel.x[p][v][r][fromCustomer][j].get(GRB.DoubleAttr.X) == 1){
                                            customers.add(j);
                                            fromCustomer = j;
                                            break;
                                        }
                                    }
                                }
                                currentTrip.setCustomers(customers);
                                currentTrip.setTripIndex(tripList[p][data.vehicles[v].vehicleType.vehicleTypeID].size());
                                tripList[p][data.vehicles[v].vehicleType.vehicleTypeID].add(currentTrip);
                            }
                        }
                    }
                }
            }
        }
    }

    private static ArrayList<Trip>[][] setTripList(){
        ArrayList<Trip>[][] tripList = new ArrayList[data.numberOfPeriods][data.numberOfVehicleTypes] ;
        for (int p = 0; p < data.numberOfPeriods; p ++){
            for (int vt = 0; vt < data.numberOfVehicleTypes; vt++){
                tripList[p][vt] = new ArrayList<Trip>();
            }
        }
        return tripList;
    }
}
