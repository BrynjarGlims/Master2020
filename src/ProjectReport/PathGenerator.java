package ProjectReport;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PathGenerator {


    private DataMIP dataMIP;

    public PathGenerator(DataMIP dataMIP) {
        this.dataMIP = dataMIP;
    }

    private Customer[] sortCustomerByVisitTime(int period) {
        Customer[] customers = Arrays.copyOf(dataMIP.customers, dataMIP.numCustomers);
        Customer[] reduced = Arrays.stream(customers).filter(c -> c.visitDays[period] == 1).toArray(Customer[]::new);
        Arrays.stream(reduced).forEach(c -> c.setCurrentPeriod(period));
        Customer[] sorted = Arrays.stream(reduced).sorted(Comparator.comparing(Customer::getEndTimeWindow)).toArray(Customer[]::new);
        return sorted;
    }

    private HashSet<Path> generatePaths(int period, VehicleType vehicleType) {
        Customer[] customers = sortCustomerByVisitTime(period);
        double[] latestTimes = new double[customers.length];
        double[] earliestTimes = new double[customers.length];
        for (int i = 0; i < customers.length; i++) {
            earliestTimes[i] = customers[i].getTimeWindow(period)[0];
            latestTimes[i] = customers[i].getTimeWindow(period)[1];
        }
        HashSet<Path> paths = new HashSet<>();
        AtomicInteger pathIndex = new AtomicInteger(0);
        for (int i = 0; i < customers.length; i++) {
            ArrayList<Integer> visitList = new ArrayList<>();
            paths.addAll(createPaths(i, -1, visitList, customers, latestTimes, earliestTimes, vehicleType, 0, vehicleType.capacity, period, pathIndex));
        }
        return paths;
    }

    private HashSet<Path> createPaths(int newVisit, int lastVisit, ArrayList<Integer> visited, Customer[] customers, double[] latestTimes,
                                      double[] earliestTimes, VehicleType vehicle, double currentTime, double remainingCapacity, int period, AtomicInteger pathIndex) {
        HashSet<Path> paths = new HashSet<>();
        ArrayList<Integer> updatedVisit = new ArrayList<>(visited);
        updatedVisit.add(newVisit);
        paths.add(createPath(updatedVisit, customers, vehicle, period, latestTimes, earliestTimes, pathIndex));

        double travelTime = lastVisit == -1 ? customers[newVisit].distanceToDepot : dataMIP.getDistance(customers[lastVisit], customers[newVisit]); //checks if it is start of route
        double newTime = Math.max(currentTime + travelTime, earliestTimes[newVisit]) + customers[newVisit].fixedUnloadingTime;
        double newRemainingCapacity = remainingCapacity + customers[newVisit].minAmountProductQuantity;
        HashSet<Integer> possibleVisits = getPossibleVisits(customers, latestTimes, earliestTimes, updatedVisit, newTime, newRemainingCapacity);
        for (int c : possibleVisits) {
            paths.addAll(createPaths(c, newVisit, updatedVisit, customers, latestTimes, earliestTimes, vehicle, newTime, newRemainingCapacity, period, pathIndex));
        }
        return paths;
    }

    private Path createPath(ArrayList<Integer> visits, Customer[] customers, VehicleType vehicle, int period, double[] latestTimes, double[] earliestTimes, AtomicInteger pathIndex) {
        Customer[] route = new Customer[visits.size()];
        for (int c = 0; c < visits.size(); c++) {
            route[c] = customers[visits.get(c)];
        }
        double[] earliestStartInterval = getStartTimeInterval(visits, customers, latestTimes, earliestTimes);
        Path p = new Path(pathIndex.getAndIncrement(), route, vehicle, period, earliestStartInterval[0], earliestStartInterval[1]);
        setDurationAndCost(p);
        return p;
    }

    private void setDurationAndCost(Path p) {
        double distance = 0;
        double duration = p.earliestStartTime;

        distance += p.customers[0].distanceToDepot;
        duration += p.customers[0].distanceToDepot;

        for (int i = 0 ; i < p.customers.length - 1 ; i++){
            distance += dataMIP.getDistance(p.customers[i], p.customers[i + 1]);
            duration += p.customers[i].fixedUnloadingTime;
            duration = Math.max(p.customers[i + 1].getTimeWindow(p.period)[0], duration + dataMIP.getDistance(p.customers[i], p.customers[i + 1]));
        }

        distance += p.customers[p.customers.length - 1].distanceToDepot;
        duration += p.customers[p.customers.length - 1].fixedUnloadingTime + p.customers[p.customers.length - 1].distanceToDepot;
        duration = duration - p.earliestStartTime;
        p.setDuration(distance, duration);
    }

    private double[] getStartTimeInterval(ArrayList<Integer> visits, Customer[] customers, double[] latestTimes, double[] earliestTimes){
        double startTime = Math.max(earliestTimes[visits.get(0)] - customers[visits.get(0)].distanceToDepot, 0);
        double timeBuffer = latestTimes[visits.get(0)] - Math.max(earliestTimes[visits.get(0)], customers[visits.get(0)].distanceToDepot);
        double newTimeBuffer;
        double totalTravelTime = customers[visits.get(0)].distanceToDepot;
        double latestPossibleVisitTime;
        double reduceBuffer;
        for (int i = 1; i < visits.size() ; i++){
            totalTravelTime += customers[visits.get(i - 1)].fixedUnloadingTime + dataMIP.getDistance(customers[visits.get(i)], customers[visits.get(i - 1)]);
            latestPossibleVisitTime = Math.min(latestTimes[visits.get(i)], (startTime + totalTravelTime + timeBuffer));
            reduceBuffer = (startTime + totalTravelTime + timeBuffer) - latestTimes[visits.get(i)];
            timeBuffer = reduceBuffer > 0 ? timeBuffer - reduceBuffer : timeBuffer;
            newTimeBuffer = Math.max(0, Math.min(latestPossibleVisitTime - earliestTimes[visits.get(i)], timeBuffer)); //max: cant go negative, min: cant increase remaining time
            startTime += timeBuffer - newTimeBuffer;
           if (newTimeBuffer == 0){
               return new double[]{startTime, startTime};
           }
            timeBuffer = newTimeBuffer;
        }

        totalTravelTime = totalTravelTime + customers[visits.get(visits.size() - 1)].fixedUnloadingTime + customers[visits.get(visits.size() - 1)].distanceToDepot;
        timeBuffer = timeBuffer - Math.max(0, startTime + totalTravelTime + timeBuffer - dataMIP.latestInTime);
        return new double[]{startTime, startTime+timeBuffer};
    }

    private HashSet<Integer> getPossibleVisits(Customer[] customers, double[] latestTimes, double[] earliestTimes, ArrayList<Integer> visited, double currentTime, double remainingCapacity) {
        HashSet<Integer> possibleVisits = new HashSet<>();
        double distance;
        for (int i = 0; i < customers.length; i++) { //can be quicker ways to check this. currently looking through all customers many times, inefficient
            distance = dataMIP.getDistance(customers[visited.get(visited.size() - 1)], customers[i]);
            if (customers[i].minAmountProductQuantity > remainingCapacity) {
                continue;
            }
            if (latestTimes[i] < currentTime + distance) {
                continue;
            }
            if (Math.max(currentTime + distance, earliestTimes[i]) + customers[i].fixedUnloadingTime + customers[i].distanceToDepot > dataMIP.latestInTime) {
                continue;
            }
            possibleVisits.add(i);
        }
        possibleVisits.removeAll(visited);
        return possibleVisits;
    }

    public Map<Integer, Map<Integer, HashSet<Path>>> generateAllPaths() {
        Map<Integer, Map<Integer, HashSet<Path>>> periodVehiclePathMap = new HashMap<>();
        for (int i = 0; i < dataMIP.numPeriods; i++) {
            periodVehiclePathMap.put(i, new HashMap<>());
            for (VehicleType v : dataMIP.vehicleTypes) {
                periodVehiclePathMap.get(i).put(v.type, generatePaths(i, v));
            }
        }
        return periodVehiclePathMap;
    }

}
