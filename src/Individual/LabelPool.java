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


    public void generateFirstLabel(int numberOfVehicles, double arcCost, int periodID, int vehicleTypeID) {
        this.labels.add(new Label(numberOfVehicles, arcCost, data, listOfTrips, tripNumber, orderDistribution,
                periodID, vehicleTypeID));
    }


    public void generateLabels(LabelPool previousLabelPool, double arcCost) {
        for (Label label : previousLabelPool.getLabels()) {
            createNewLabels(label, arcCost); //todo:implement load infeasability
            //System.out.println("Label expanded");
        }
    }

    public void createNewLabels(Label predecessorLabel, double arcCost){


        int vehicleCostOrderNumber = 0;
        // Generate labels by adding cost on already existing vehicles
        while ( predecessorLabel.labelEntries[vehicleCostOrderNumber].vehicleCost != 0){
            labels.add(new Label(predecessorLabel, vehicleCostOrderNumber, arcCost));
            if ( vehicleCostOrderNumber == predecessorLabel.labelEntries.length-1)
                break;
            vehicleCostOrderNumber++;
        }

        // Creating labels based on a new vehicle in use.
        if (predecessorLabel.labelEntries[vehicleCostOrderNumber].vehicleCost == 0){
            labels.add(new Label(predecessorLabel, vehicleCostOrderNumber, arcCost));
        }
    }

    public void removeDominated(){
        HashSet<Label> tempLabelSet = (HashSet<Label>) labels.clone();
        for(Label firstLabel : tempLabelSet){
            for(Label secondLabel : labels){
                if (firstLabel.equals(secondLabel)){
                    continue;
                }
                if (checkDominance(firstLabel, secondLabel)){
                    labels.remove(secondLabel);
                    break;
                }
            }
        }
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
            sum += deltaFunction(firstLabelEntries[k].getTravelTimeValue(),
                    secondLabelEntries[k].getTravelTimeValue());
        }
        return sum;
    }


    public double deltaFunction(double firstVehicleTravelTime, double secondVehicleTravelTime) {
        return Math.max(0, Math.min(Parameters.maxJourneyDuration, firstVehicleTravelTime)
                - Math.min(Parameters.maxJourneyDuration, secondVehicleTravelTime));
    }

}