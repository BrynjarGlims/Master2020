package Master2020.PR;

import Master2020.DataFiles.Data;
import Master2020.Individual.Individual;
import Master2020.MIP.ImprovedJourneyCombinationModel;
import Master2020.MIP.JourneyCombinationModel;
import Master2020.ProductAllocation.OrderDistribution;
import gurobi.GRB;
import gurobi.GRBException;
import Master2020.Individual.Trip;
import Master2020.Individual.Journey;
import Master2020.Individual.GiantTour;

import java.util.ArrayList;
import java.util.HashMap;


public class ModelConverter {

    private static OrderDistribution orderDistribution;
    private static Individual individual;
    private static ArcFlowModel arcFlowModel;
    private static JourneyBasedModel journeyBasedModel;
    private static PathFlowModel pathFlowModel;
    private static JourneyCombinationModel journeyCombinationModel;
    private static Data data;
    private static DataMIP dataMIP;
    private static JourneyCombinationModel jcm;
    private static ImprovedJourneyCombinationModel ijcm;

    public static void initializeOrderDistributionFromModel(ImprovedJourneyCombinationModel model) throws GRBException {
        ijcm = model;
        data = model.dataMIP.newData;
        dataMIP = model.dataMIP;
        orderDistribution = model.getOrderDistribution();
        orderDistribution.makeDistributionFromImprovedJourneyBasedModel(ijcm.u, ijcm.q, ijcm.dataMIP);

        //individual is not implemented for this model as it is never used.
        //individual = model.getIndividual();
        //set giant tour chromosome and support strucutres in new individual
        //initializeGiantTourInIndividualForJCM(model.getJourneys());

        //individual.setOrderDistribution(orderDistribution);
    }

    public static void initializeIndividualFromModel(JourneyCombinationModel model) throws GRBException {
        jcm = model;
        data = model.dataMIP.newData;
        dataMIP = model.dataMIP;
        orderDistribution = model.getOrderDistribution();
        orderDistribution.makeDistributionFromModel(jcm.u, jcm.q, jcm.dataMIP);
        individual = model.getIndividual();
        //set giant tour chromosome and support strucutres in new individual
        initializeGiantTourInIndividualForJCM(model.getJourneys());

        individual.setOrderDistribution(orderDistribution);
    }


    public static void initializeIndividualFromJourneyCombinationModel(JourneyCombinationModel jcm) throws GRBException {
        journeyCombinationModel = jcm;
        data = journeyBasedModel.dataMIP.newData;
        orderDistribution = jcm.getOrderDistribution();
        orderDistribution.makeDistributionFromJourneyBasedModel(journeyBasedModel);
        individual = jcm.getIndividual();
        //set giant tour chromosome and support strucutres in new individual
        initializeGiantTourInIndividualForJBM();
        individual.setOrderDistribution(orderDistribution);
    }

    public static void initializeIndividualFromJourneyBasedModel(JourneyBasedModel jbm) throws GRBException {
        journeyBasedModel = jbm;
        data = journeyBasedModel.dataMIP.newData;
        orderDistribution = jbm.getOrderDistribution();
        orderDistribution.makeDistributionFromJourneyBasedModel(journeyBasedModel);
        individual = jbm.getIndividual();
        //set giant tour chromosome and support strucutres in new individual
        initializeGiantTourInIndividualForJBM();
        individual.setOrderDistribution(orderDistribution);
    }

    public static void initializeIndividualFromPathFlowModel(PathFlowModel pfm) throws GRBException {
        pathFlowModel = pfm;
        data = pathFlowModel.dataMIP.newData;
        orderDistribution = pfm.getOrderDistribution();
        orderDistribution.makeDistributionFromPathFlowModel(pathFlowModel);
        individual = pfm.getIndividual();
        //set giant tour chromosome and support strucutres in new individual
        initializeGiantTourInIndividualForPFM();
        individual.setOrderDistribution(orderDistribution);
    }


    //function working on old ArcFlowModel
    public static void initializeIndividualFromArcFlowModel(ArcFlowModel afm) throws GRBException {
        arcFlowModel = afm;
        data = arcFlowModel.dataMIP.newData;
        orderDistribution = afm.getOrderDistribution();
        orderDistribution.makeDistributionFromArcFlowModel(arcFlowModel);
        individual = afm.getIndividual();
        //set giant tour chromosome and support strucutres in new individual
        initializeGiantTourInIndividualForAFM();
        individual.setOrderDistribution(orderDistribution);
    }



    private static void initializeGiantTourInIndividualForAFM() throws GRBException {
        ArrayList<Trip>[][] tripList = initializeTripList();
        updateTripListAFM(tripList);
        ArrayList<Integer>[][] giantTour = initializeGiantTourChromosome();
        updateGiantTourChromosome(giantTour,tripList);
        HashMap<Integer, HashMap<Integer, Trip>> tripMap = getTripMap(tripList);
        individual.setTripMap(tripMap);
        individual.setTripList(tripList);
        individual.setGiantTour(createGiantTour(giantTour));
        individual.journeyList = generateJourneyListFromTripList(tripList);
    }

    private static void initializeGiantTourInIndividualForJCM(ArrayList<Master2020.Individual.Journey>[][] journeys) throws GRBException {
        ArrayList<Trip>[][] tripList = initializeTripList();
        updateTripListJCM(tripList, journeys);
        ArrayList<Integer>[][] giantTour = initializeGiantTourChromosome();
        updateGiantTourChromosome(giantTour,tripList);
        HashMap<Integer, HashMap<Integer, Trip>> tripMap = getTripMap(tripList);
        individual.setTripMap(tripMap);
        individual.setTripList(tripList);
        individual.setGiantTour(createGiantTour(giantTour));
        individual.journeyList = generateJourneyListFromTripList(tripList);
    }

    private static void initializeGiantTourInIndividualForIJCM(ArrayList<Master2020.Individual.Journey>[][] journeys) throws GRBException {
        ArrayList<Trip>[][] tripList = initializeTripList();
        updateTripListJCM(tripList, journeys);
        ArrayList<Integer>[][] giantTour = initializeGiantTourChromosome();
        updateGiantTourChromosome(giantTour,tripList);
        HashMap<Integer, HashMap<Integer, Trip>> tripMap = getTripMap(tripList);
        individual.setTripMap(tripMap);
        individual.setTripList(tripList);
        individual.setGiantTour(createGiantTour(giantTour));
        individual.journeyList = generateJourneyListFromTripList(tripList);
    }

    private static void initializeGiantTourInIndividualForJBM() throws GRBException {
        ArrayList<Trip>[][] tripList = initializeTripList();
        updateTripListJBM(tripList);
        ArrayList<Integer>[][] giantTour = initializeGiantTourChromosome();
        updateGiantTourChromosome(giantTour,tripList);
        HashMap<Integer, HashMap<Integer, Trip>> tripMap = getTripMap(tripList);
        individual.setTripMap(tripMap);
        individual.setTripList(tripList);
        individual.setGiantTour(createGiantTour(giantTour));
        individual.journeyList = generateJourneyListFromTripList(tripList);
    }

    private static void initializeGiantTourInIndividualForPFM() throws GRBException {
        ArrayList<Trip>[][] tripList = initializeTripList();
        updateTripListPFM(tripList);
        ArrayList<Integer>[][] giantTour = initializeGiantTourChromosome();
        updateGiantTourChromosome(giantTour,tripList);
        HashMap<Integer, HashMap<Integer, Trip>> tripMap = getTripMap(tripList);
        individual.setTripMap(tripMap);
        individual.setTripList(tripList);
        individual.setGiantTour(createGiantTour(giantTour));
        individual.journeyList = generateJourneyListFromTripList(tripList);
    }

    private static ArrayList<Journey>[][] generateJourneyListFromTripList(ArrayList<Trip>[][] tripList){
        ArrayList<Journey>[][] journeys =  new ArrayList[data.numberOfPeriods][data.numberOfVehicleTypes];
        Journey newJourney;
        for( int p = 0; p < data.numberOfPeriods; p++){
            for (int vt = 0; vt < data.numberOfVehicleTypes; vt++){
                journeys[p][vt] = new ArrayList<Journey>();
                if (tripList[p][vt].size() == 0){
                    continue;
                }
                boolean firstTrip = true;
                newJourney = new Journey(data, tripList[p][vt].get(0).period, tripList[p][vt].get(0).vehicleType, tripList[p][vt].get(0).vehicleID);
                newJourney.addTrip(tripList[p][vt].get(0));
                journeys[p][vt].add(newJourney);
                for (Trip t : tripList[p][vt]){
                    if (firstTrip){
                        firstTrip = false;
                        continue;
                    }
                    if (t.vehicleID == journeys[p][vt].get(journeys[p][vt].size()-1).vehicleId){
                        journeys[p][vt].get(journeys[p][vt].size()-1).addTrip(t);
                    } else{
                        newJourney = new Journey(data, t.period, t.vehicleType, t.vehicleID);
                        newJourney.addTrip(t);
                        journeys[p][vt].add(newJourney);
                    }
                }
            }
        }
        return journeys;
    }


    private static GiantTour createGiantTour(ArrayList<Integer>[][] giantTour){
        GiantTour gt = new GiantTour(data);
        gt.setChromosome(giantTour);
        return gt;
    }

    private static HashMap<Integer, HashMap<Integer, Trip>> getTripMap(ArrayList<Trip>[][] tripList) {
        HashMap<Integer, HashMap<Integer, Trip>> tripMap = new HashMap<Integer, HashMap<Integer, Trip>>();
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

    private static void updateTripListJCM(ArrayList<Trip>[][] tripList, ArrayList<Master2020.Individual.Journey>[][] journeys) throws GRBException {
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int j = 0; j < journeys[d][data.vehicles[v].vehicleType.vehicleTypeID].size(); j++) {
                    if (Math.round(jcm.gamma[d][v][j].get(GRB.DoubleAttr.X)) == 1){
                        for(Trip t : journeys[d][data.vehicles[v].vehicleType.vehicleTypeID].get(j).trips){
                            tripList[d][data.vehicles[v].vehicleType.vehicleTypeID].add(t);
                        }
                    }
                }
            }
        }
    }

    private static void updateTripListJBM(ArrayList<Trip>[][] tripList) throws GRBException {
        Trip currentTrip;
        ArrayList<Integer> customers;

        for (int d = 0; d < journeyBasedModel.dataMIP.numPeriods; d++) {
            for (int v = 0; v < journeyBasedModel.dataMIP.numVehicles; v++) {
                for (Master2020.PR.Journey r : journeyBasedModel.dataMIP.journeyMap.get(d).get(journeyBasedModel.dataMIP.vehicles[v].vehicleType.type)) {
                    if ( Math.round(journeyBasedModel.gamma[d][v][r.journeyId].get(GRB.DoubleAttr.X)) == 1){
                        for(Path path : r.paths){
                            currentTrip = new Trip();
                            customers = new ArrayList<Integer>();
                            currentTrip.initialize(d, data.vehicles[v].vehicleType.vehicleTypeID, v);
                            for (Customer customer : path.customers){
                                customers.add(customer.customerID);
                            }
                            currentTrip.setCustomers(customers);
                            currentTrip.setTripIndex(tripList[r.period][journeyBasedModel.dataMIP.vehicles[v].vehicleType.type].size());
                            tripList[r.period][journeyBasedModel.dataMIP.vehicles[v].vehicleType.type].add(currentTrip);
                        }
                    }
                }
            }
        }
    }

    private static void updateTripListPFM(ArrayList<Trip>[][] tripList) throws GRBException {
        Trip currentTrip;
        ArrayList<Integer> customers;

        for (int d = 0; d < pathFlowModel.dataMIP.numPeriods; d++) {
            for (int v = 0; v < pathFlowModel.dataMIP.numVehicles; v++) {
                for (int r = 0; r < pathFlowModel.dataMIP.numTrips; r++) {
                    for (Path path : pathFlowModel.dataMIP.pathMap.get(d).get(pathFlowModel.dataMIP.vehicles[v].vehicleType.type)) {
                        if (Math.round(pathFlowModel.lambda[d][v][r][path.pathId].get(GRB.DoubleAttr.X)) == 1) {
                            currentTrip = new Trip();
                            customers = new ArrayList<Integer>();
                            currentTrip.initialize(d, data.vehicles[v].vehicleType.vehicleTypeID, v);
                            for (Customer customer : path.customers) {
                                customers.add(customer.customerID);
                            }
                            currentTrip.setCustomers(customers);
                            currentTrip.setTripIndex(tripList[d][pathFlowModel.dataMIP.vehicles[v].vehicleType.type].size());
                            tripList[d][pathFlowModel.dataMIP.vehicles[v].vehicleType.type].add(currentTrip);
                        }
                    }
                }
            }
        }
    }



    private static void updateTripListAFM(ArrayList<Trip>[][] tripList) throws GRBException {
        Trip currentTrip;
        int fromCustomer;
        ArrayList<Integer> customers;

        for ( int p = 0; p < data.numberOfPeriods; p ++ ){
            for (int v = 0; v < data.numberOfVehicles; v ++){
                for (int r = 0; r < data.numberOfTrips; r++){
                    if (Math.round(arcFlowModel.z[p][v][r].get(GRB.DoubleAttr.X)) == 1){
                        for( int i = 0; i < data.numberOfCustomers; i++){
                            if (Math.round(arcFlowModel.x[p][v][r][data.numberOfCustomers][i].get(GRB.DoubleAttr.X)) == 1){
                                currentTrip = new Trip();
                                customers = new ArrayList<>();
                                customers.add(i);
                                currentTrip.initialize(p, data.vehicles[v].vehicleType.vehicleTypeID, v);
                                fromCustomer = i;
                                while (Math.round(arcFlowModel.x[p][v][r][fromCustomer][data.numberOfCustomers+1].get(GRB.DoubleAttr.X)) == 0){
                                    for (int j = 0; j < data.numberOfCustomers; j++){
                                        if( Math.round(arcFlowModel.x[p][v][r][fromCustomer][j].get(GRB.DoubleAttr.X)) == 1){
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

    private static void checkIfEmpty( ArrayList<Trip> tripList){
        for(Trip trip : tripList){
            if (trip.getCustomers().size() == 0){
                System.out.println("Empty trip is found");


            }
        }

    }

    private static ArrayList<Trip>[][] initializeTripList(){
        ArrayList<Trip>[][] tripList = new ArrayList[data.numberOfPeriods][data.numberOfVehicleTypes] ;
        for (int p = 0; p < data.numberOfPeriods; p ++){
            for (int vt = 0; vt < data.numberOfVehicleTypes; vt++){
                tripList[p][vt] = new ArrayList<Trip>();
            }
        }
        return tripList;
    }
}


