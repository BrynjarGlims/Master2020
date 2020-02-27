package Individual;

import DataFiles.*;

import java.util.ArrayList;

public class LabelPool {

    ArrayList<Label> labels;
    Data data;
    Label bestLabel = null;
    ArrayList<ArrayList<Integer>> listOfTrips;
    int tripNumber;
    double[][] orderDistribution;

    public LabelPool (Data data, ArrayList<ArrayList<Integer>> listOfTrips, int tripNumber, double[][] orderDistribution){
        this.labels = new ArrayList<Label>();
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

        double additionalLoadInfeasibility = 0; // TODO: 13.02.2020 IMPLEMENT
        int vehicleCostOrderNumber = 0;
        // Generate labels by adding cost on already existing vehicles
        while ( predecessorLabel.labelEntries[vehicleCostOrderNumber].vehicleCost != 0){
            labels.add(new Label(predecessorLabel, vehicleCostOrderNumber, arcCost));
            if ( vehicleCostOrderNumber == predecessorLabel.labelEntries.length-1)
                break;
            vehicleCostOrderNumber++;
        }

        // Creating labels based on a new vehicle in use.
        if (predecessorLabel.labelEntries[predecessorLabel.labelEntries.length-1].vehicleCost == 0){
            labels.add(new Label(predecessorLabel, vehicleCostOrderNumber, arcCost));
        }
    }

    public void removeDominated(){
        for(int i = 0; i < labels.size() - 1; i++ ){
            for (int j = i; j < labels.size(); j++){
                this.isDominated(i,j);
            }
        }
    }

    public void isDominated(int i, int j){
        if (checkDominance(i,j)){
            labels.remove(j);
            //System.out.println("Label removed!");
        }
        else if (checkDominance(j,i)){
            labels.remove(i);
            //System.out.println("Label removed!");
        }

    }

    public  ArrayList<Label> getLabels(){
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


    public boolean checkDominance(int i, int j){
        double firstLabelValue = 0;

        for(int k = 0; k < labels.get(i).labelEntries.length; k++){
            firstLabelValue += deltaFunction(labels.get(i).labelEntries[k].getTravelTimeValue(),
                    labels.get(j).labelEntries[k].getTravelTimeValue());
        }
        firstLabelValue *= Parameters.initialOvertimePenalty;
        firstLabelValue += labels.get(i).costOfLabel;

        double secondLabelValue = labels.get(j).costOfLabel;
        return firstLabelValue <= secondLabelValue;
    }



    public double deltaFunction(double firstVehicleTravelTime, double secondVehicleTravelTime) {
        return Math.max(0, Math.min(Parameters.maxJourneyDuration, firstVehicleTravelTime)
                - Math.min(Parameters.maxJourneyDuration, secondVehicleTravelTime));
    }

}