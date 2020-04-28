package Master2020.PR;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class JourneyGenerator {

    private DataMIP dataMIP;

    public JourneyGenerator(DataMIP dataMIP){
        this.dataMIP = dataMIP;
    }

    private HashSet<Journey> generateJourneys(int period, VehicleType vehicleType){
        AtomicInteger journeyId = new AtomicInteger(0);
        HashSet<Path> paths = dataMIP.pathMap.get(period).get(vehicleType.type);
        List<Path> sortedPaths = new ArrayList<>(paths);
        Collections.sort(sortedPaths);
        HashSet<Journey> allJourneys = new HashSet<>();

        double currentTime;
        for (Path p : sortedPaths){
            int[] visited = new int[dataMIP.numCustomers];
            for (Customer c : p.customers){
                visited[c.customerID] = 1;
            }
            currentTime = p.earliestStartTime + p.duration;

            List<Path> usedPaths = new ArrayList<>();
            usedPaths.add(p);
            allJourneys.addAll(createJourneys(usedPaths, sortedPaths, visited, currentTime, period, vehicleType, journeyId));
            usedPaths.remove(p);
        }
        return allJourneys;
    }


    private HashSet<Journey> createJourneys(List<Path> usedPaths, List<Path> sortedPaths, int[] visitedCustomers, double currentTime, int period, VehicleType vehicleType, AtomicInteger journeyId){
        HashSet<Journey> journeys = new HashSet<>();
        journeys.add(createJourney(usedPaths, period, vehicleType, journeyId));
        currentTime += dataMIP.loadingTime[vehicleType.type];
        HashSet<Path> viablePaths = viablePaths(sortedPaths, visitedCustomers, usedPaths.size(), currentTime);


        double newCurrentTime;
        for (Path p : viablePaths){
            int[] newVisitedCustomers = visitedCustomers.clone();
            for (Customer c : p.customers){
                newVisitedCustomers[c.customerID] = 1;
            }
            newCurrentTime = Math.max(p.earliestStartTime, currentTime) + p.duration;
            usedPaths.add(p);
            journeys.addAll(createJourneys(usedPaths, sortedPaths, newVisitedCustomers, newCurrentTime, period, vehicleType, journeyId));
            usedPaths.remove(p);
        }
        return journeys;
    }


    private HashSet<Path> viablePaths(List<Path> sortedPaths, int[] visitedCustomers, int numTrips, double currentTime){
        HashSet<Path> viablePaths = new HashSet<>();

        if (numTrips >= dataMIP.numTrips){
            return viablePaths;
        }
        int startIndex = 0;
        while ( startIndex < sortedPaths.size() && currentTime > sortedPaths.get(startIndex).latestStartTime) {
            startIndex++;
        }
        boolean valid;
        for (int i = startIndex ; i < sortedPaths.size() ; i++){
            valid = true;
            for (Customer c : sortedPaths.get(i).customers){
                if (visitedCustomers[c.customerID] == 1){
                    valid = false;
                    break;
                }
            }
            if (valid){
                viablePaths.add(sortedPaths.get(i));
            }
        }
        return viablePaths;
    }

    private Journey createJourney(List<Path> visited, int period, VehicleType vehicleType, AtomicInteger journeyId){
        double duration = 0;
        double cost = 0;
        int numTrips = visited.size();
        Path[] paths = new Path[numTrips];
        List<Customer> customers = new ArrayList<>();
        int index = 0;
        for (Path p : visited){
            duration += p.distance;
            cost += p.cost;
            customers.addAll(Arrays.asList(p.customers));
            paths[index] = p;
            index++;
        }
        Customer[] customersArray = new Customer[customers.size()];
        for (int i = 0 ; i < customers.size() ; i++){
            customersArray[i] = customers.get(i);
        }
        return new Journey(journeyId.getAndIncrement(), customersArray, numTrips, paths, duration, cost, period, vehicleType);
    }


    public Map<Integer, Map<Integer, HashSet<Journey>>> generateAllJourneys() {
        Map<Integer, Map<Integer, HashSet<Journey>>> periodVehicleJourneyMap = new HashMap<>();
        for (int i = 0; i < dataMIP.numPeriods; i++) {
            periodVehicleJourneyMap.put(i, new HashMap<>());
            for (VehicleType v : dataMIP.vehicleTypes) {
                periodVehicleJourneyMap.get(i).put(v.type, generateJourneys(i, v));
            }
        }
        return periodVehicleJourneyMap;
    }
}
