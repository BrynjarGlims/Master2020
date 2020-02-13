package Individual;

import DataFiles.*;

import java.util.ArrayList;
import java.util.stream.DoubleStream;

public class LabelPool {

    ArrayList<Label> labels =  new ArrayList<Label>();
    Data data;
    Label bestLabel = null;
    ArrayList<ArrayList<Integer>> listOfTrips;
    int tripNumber;
    double[][] orderDistribution;

    public LabelPool (Data data, ArrayList<ArrayList<Integer>> listOfTrips, int tripNumber){
        this.labels = new ArrayList<Label>();
        this.data = data;
        this.listOfTrips = listOfTrips;
        this.tripNumber = tripNumber;

    }




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
        while ( predecessorLabel.vehicleTravelCost[vehicleID] != 0){
            double[] newVehicleTravelTime = predecessorLabel.vehicleTravelCost.clone();
            newVehicleTravelTime[vehicleID] += arcCost;
            double newLoadInFeasibility = predecessorLabel.loadInfeasibility + additionalLoadInfeasibility;

            labels.add(new Label(predecessorLabel));
            vehicleID++;

        }
        // Creating labels based on a new vehicle in use.
        if (predecessorLabel.vehicleTravelCost[predecessorLabel.vehicleTravelCost.length] == 0){
            double[] newVehicleTravelTime = predecessorLabel.vehicleTravelCost.clone();
            newVehicleTravelTime[vehicleID] += arcCost;
            double newLoadInFeasibility = predecessorLabel.loadInfeasibility + additionalLoadInfeasibility;
            labels.add(new Label(predecessorLabel));
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

        double firstLabelValue = 0;

        for(int k = 0; k < labels.get(i).vehicleTravelCost.length; k++){
            firstLabelValue += deltaFunction(labels.get(i).vehicleTravelCost.length,
                    labels.get(j).vehicleTravelCost.length);
        }
        firstLabelValue = Parameters.initialOvertimePenalty;

        firstLabelValue += labels.get(i).cost;
        double secondLabelValue = labels.get(j).cost;

        return firstLabelValue <= secondLabelValue;
    }

    public double deltaFunction(double firstVehicleTravelTime, double secondVehicleTravelTime){
        return Math.max(0, Math.min(Parameters.maxJourneyDuration, firstVehicleTravelTime)
                - Math.min(Parameters.maxJourneyDuration, secondVehicleTravelTime));

    }
}

