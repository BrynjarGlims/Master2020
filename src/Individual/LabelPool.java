package Individual;

import DataFiles.*;

import javax.swing.text.LabelView;
import java.util.ArrayList;
import java.util.HashSet;


public class LabelPool {

    HashSet<Label> labels;
    Data data;
    Label bestLabel = null;
    ArrayList<ArrayList<Integer>> listOfTrips;
    int tripNumber;
    double[][] orderDistribution;

    public LabelPool (Data data, ArrayList<ArrayList<Integer>> listOfTrips, int tripNumber, double[][] orderDistribution){
        this.labels = new HashSet<>();
        this.data = data;
        this.listOfTrips = listOfTrips;
        this.tripNumber = tripNumber;
        this.orderDistribution = orderDistribution;
    }

    public void generateFirstLabel(int numberOfVehicles, int periodID, int vehicleTypeID){
        generateFirstLabel(numberOfVehicles, periodID, vehicleTypeID, 1);
    }

    public void generateFirstLabel(int numberOfVehicles, int periodID, int vehicleTypeID, double penaltyMultiplier) {
        this.labels.add(new Label(numberOfVehicles, data, listOfTrips, tripNumber, orderDistribution,
                periodID, vehicleTypeID, penaltyMultiplier));
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
            tryToAddNewLabel(new Label(predecessorLabel, vehicleCostOrderNumber, penaltyMultiplier));
            if (vehicleCostOrderNumber == predecessorLabel.labelEntries.length-1)
                break;
            vehicleCostOrderNumber++;
        }

        // Creating labels based on a new vehicle in use.
        if (predecessorLabel.labelEntries[vehicleCostOrderNumber].vehicleCost == 0){
            tryToAddNewLabel(new Label(predecessorLabel, vehicleCostOrderNumber, penaltyMultiplier));
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
        double firstLabelValue = 0;
        firstLabelValue += calculateDeltaSumValue(firstLabel.labelEntries, secondLabel.labelEntries);
        firstLabelValue *= Parameters.initialOvertimePenalty;
        firstLabelValue += firstLabel.costOfLabel;
        double secondLabelValue = secondLabel.costOfLabel;
        return firstLabelValue <= secondLabelValue;
    }

    private double calculateDeltaSumValue(LabelEntry[] firstLabelEntries, LabelEntry[] secondLabelEntries){
        double sum = 0;
        for(int k = 0; k < firstLabelEntries.length; k++){
            sum += deltaFunction(firstLabelEntries[k].getDrivingDistance(),
                    secondLabelEntries[k].getDrivingDistance());
        }
        return sum;
    }


    public double deltaFunction(double firstVehicleTravelTime, double secondVehicleTravelTime) {
        return Math.max(0, Math.min(Parameters.maxJourneyDuration, firstVehicleTravelTime)
                - Math.min(Parameters.maxJourneyDuration, secondVehicleTravelTime));
    }

}