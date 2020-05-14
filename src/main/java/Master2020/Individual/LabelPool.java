package Master2020.Individual;

import Master2020.DataFiles.*;
import java.util.ArrayList;
import java.util.HashSet;


public class LabelPool {

    HashSet<Label> labels;
    Data data;
    Label bestLabel = null;
    ArrayList<ArrayList<Integer>> listOfTrips;
    int tripNumber;
    double[][] orderDistribution;

    double timeWarpPenalty;
    double overLoadPenalty;

    public LabelPool (Data data, ArrayList<ArrayList<Integer>> listOfTrips, int tripNumber, double[][] orderDistribution, double timeWarpPenalty, double overLoadPenalty){
        this.labels = new HashSet<>();
        this.data = data;
        this.listOfTrips = listOfTrips;
        this.tripNumber = tripNumber;
        this.orderDistribution = orderDistribution;
        this.timeWarpPenalty = timeWarpPenalty;
        this.overLoadPenalty = overLoadPenalty;
    }

    public void generateFirstLabel(int numberOfVehicles, int periodID, int vehicleTypeID){
        generateFirstLabel(numberOfVehicles, periodID, vehicleTypeID, 1, Parameters.initialTimeWarpPenalty, Parameters.initialOverLoadPenalty);
    }

    public void generateFirstLabel(int numberOfVehicles, int periodID, int vehicleTypeID, double penaltyMultiplier, double timeWarpPenalty, double overLoadPenalty) {
        this.labels.add(new Label(numberOfVehicles, data, listOfTrips, tripNumber, orderDistribution,
                periodID, vehicleTypeID, penaltyMultiplier, timeWarpPenalty, overLoadPenalty));
    }

    public void generateAndRemoveDominatedLabels(LabelPool previousLabelPool){
        generateAndRemoveDominatedLabels(previousLabelPool, 1);
    }

    public void generateAndRemoveDominatedLabels(LabelPool previousLabelPool, double penaltyMultiplier) {
        for (Label label : previousLabelPool.getLabels()) {
            addExtendedDominantLabels(label, penaltyMultiplier);
        }
    }

    private void addExtendedDominantLabels(Label predecessorLabel, double penaltyMultiplier){
        int vehicleCostOrderNumber = 0;
        // Generate labels by adding cost on already existing vehicles
        while (predecessorLabel.labelEntries[vehicleCostOrderNumber].inUse){
            tryToAddNewLabel(new Label(predecessorLabel, vehicleCostOrderNumber, penaltyMultiplier, timeWarpPenalty, overLoadPenalty));
            if (vehicleCostOrderNumber == predecessorLabel.labelEntries.length-1)
                break;
            vehicleCostOrderNumber++;
        }

        // Creating labels based on a new vehicle in use.
        if (predecessorLabel.labelEntries[vehicleCostOrderNumber].vehicleCost == 0){
            tryToAddNewLabel(new Label(predecessorLabel, vehicleCostOrderNumber, penaltyMultiplier, timeWarpPenalty, overLoadPenalty));
        }
    }

    private void tryToAddNewLabel(Label currentLabel){
        if (isNotDominated(currentLabel)){
            eliminateDominatedLabels(currentLabel);
            labels.add(currentLabel);
        }

    }

    private boolean isNotDominated(Label currentLabel){
        if (labels.isEmpty()){
            return true;
        }
        for (Label label : labels){
            if (checkDominance(label, currentLabel)){
                return false;
            }
        }
        return true;
    }

    private void eliminateDominatedLabels(Label currentLabel){
        if (labels.isEmpty()){
            return;
        }
        HashSet<Label> setOfRemovedLabels = new HashSet<Label>();
        for (Label testLabel : labels){
            if(checkDominance(currentLabel, testLabel)){
                setOfRemovedLabels.add(testLabel);
            }
        }
        labels.removeAll(setOfRemovedLabels);
    }



    public  HashSet<Label> getLabels(){
        return this.labels;
    }


    public Label findBestLabel(){
        double minCost = - Double.MAX_VALUE;
        for(Label label : labels){
            if (label.costOfLabel > minCost)
                bestLabel = label;
        }
        return bestLabel;
    }


    private boolean checkDominance(Label firstLabel, Label secondLabel){
        double firstLabelValue = firstLabel.costOfLabel;
        double secondLabelValue = secondLabel.costOfLabel;
        if (firstLabelValue > secondLabelValue){
            return false;
        }
        secondLabelValue *= Parameters.heuristicDominanceValue;
        firstLabelValue += calculateDeltaSumValue(firstLabel.labelEntries, secondLabel.labelEntries)
                *timeWarpPenalty;
        return firstLabelValue <= secondLabelValue;
    }

    private double calculateDeltaSumValue(LabelEntry[] firstLabelEntries, LabelEntry[] secondLabelEntries){
        double sum = 0;
        for(int k = 0; k < firstLabelEntries.length; k++){
            sum += deltaFunction(firstLabelEntries[k].getVehicleCurrentTime(),
                    secondLabelEntries[k].getVehicleCurrentTime());
        }
        return sum;
    }


    public double deltaFunction(double firstVehicleTravelTime, double secondVehicleTravelTime) {
        return Math.max(0,  firstVehicleTravelTime -  secondVehicleTravelTime);
    }

}