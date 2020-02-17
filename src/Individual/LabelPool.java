package Individual;

import DataFiles.*;

import java.util.ArrayList;

public class LabelPool {

    ArrayList<Label> labels =  new ArrayList<Label>();
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

/*
    public void generateFirstLabel(int numberOfVehicles, int arcCost){
        this.labels.add(new Label(numberOfVehicles, arcCost, data, listOfTrips, tripNumber, orderDistribution));
    }

    public void generateLabels(LabelPool previousLabelPool, double arcCost ) {
        for (Label label : previousLabelPool.getLabels()) {
            createNewLabels(label, arcCost); //todo:implement load infeasability
    }
    }

    public void createNewLabels(Label predecessorLabel, double arcCost){

        double additionalLoadInfeasibility = 0; // TODO: 13.02.2020 IMPLEMENT
        int vehicleID = 0;
        // Generate labels by adding cost on already existing vehicles
        while ( predecessorLabel.arcTraversalCost[vehicleID] != 0){
            double[] newVehicleTravelTime = predecessorLabel.arcTraversalCost.clone();
            newVehicleTravelTime[vehicleID] += arcCost;
            double newLoadInFeasibility = predecessorLabel.loadInfeasibility + additionalLoadInfeasibility;
            // TODO: 14.02.2020 Implement load infeasability

            labels.add(new Label(predecessorLabel, vehicleID, arcCost));

            if ( vehicleID == predecessorLabel.arcTraversalCost.length-1)
                break;
            vehicleID++;

        }
        // Creating labels based on a new vehicle in use.
        if (predecessorLabel.arcTraversalCost[predecessorLabel.arcTraversalCost.length-1] == 0){
            labels.add(new Label(predecessorLabel, vehicleID, arcCost));
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
        }
        else if (checkDominance(j,i)){
            labels.remove(i);
        }

    }

    public  ArrayList<Label> getLabels(){
        return this.labels;
    }


    public Label findBestLabel(){
        double minCost = - Double.MAX_VALUE;
        for(Label label : labels){
            if (label.cost > minCost)
                bestLabel = label;
        }
        return bestLabel;
    }


    public boolean checkDominance(int i, int j){
        double firstLabelValue = labels.get(i).cost;
        double secondLabelValue = labels.get(j).cost;
        for(int k = 0; k < labels.get(i).vehicleTravelTime.length; k++){
            firstLabelValue += Parameters.initialOvertimePenalty*deltaFunction(labels.get(i).vehicleTravelTime[k],
                    labels.get(j).vehicleTravelTime[k]);


        double firstLabelValue = 0;

        for(int k = 0; k < labels.get(i).arcTraversalCost.length; k++){
            firstLabelValue += deltaFunction(labels.get(i).arcTraversalCost.length,
                    labels.get(j).arcTraversalCost.length);
        }
        firstLabelValue = Parameters.initialOvertimePenalty;

        firstLabelValue += labels.get(i).cost;
        double secondLabelValue = labels.get(j).cost;

        return firstLabelValue <= secondLabelValue;
    }

    public double deltaFunction(double firstVehicleTravelTime, double secondVehicleTravelTime) {
        return Math.max(0, Math.min(Parameters.maxJourneyDuration, firstVehicleTravelTime)
                - Math.min(Parameters.maxJourneyDuration, secondVehicleTravelTime));

    }


 */
}