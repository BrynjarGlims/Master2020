package Individual;

import DataFiles.Parameters;

import java.util.ArrayList;
import java.util.Arrays;

public class LabelPool {

    ArrayList<Label> labels =  new ArrayList<Label>();

    public LabelPool (){
        this.labels = new ArrayList<Label>();

        
    }


    public void generateLabels(ArrayList<Label> previousLabels, double arcCost, int numberOfVehicles, double addidionalLoadInfeasability){
        this.labels = new ArrayList<Label>();
        for (Label label : previousLabels){
            createNewLables(label, arcCost, numberOfVehicles, addidionalLoadInfeasability);
    }
    }

    public void createNewLables(Label predecessorLabel, double arcCost, int numberOfVehicles,
                                double addidionalLoadInfeasability ){
        Label tempLabel;
        int i = 0;
        while ( predecessorLabel.vehicleTravelTime[i] != 0){
            int[] newVehicleTravelTime = predecessorLabel.vehicleTravelTime.clone();
            newVehicleTravelTime[i] += arcCost;
            double newLoadInFeasability = predecessorLabel.loadInfeasibility + addidionalLoadInfeasability;
            labels.add(new Label(newVehicleTravelTime, newLoadInFeasability, predecessorLabel));

        }
        if (predecessorLabel.vehicleTravelTime[predecessorLabel.vehicleTravelTime.length] == 0){

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

    public boolean checkDominance(int i, int j){
        double firstLabelValue = labels.get(i).cost;
        double secondLabelValue = labels.get(j).cost;
        for(int k = 0; k < labels.get(i).vehicleTravelTime.size(); k++){
            firstLabelValue += Parameters.initialOvertimePenalty*deltaFunction(labels.get(i).vehicleTravelTime.get(k),
                    labels.get(j).vehicleTravelTime.get(k));
        }
        return firstLabelValue <= secondLabelValue;
    }

    public double deltaFunction(double firstVehicleTravelTime, double secondVehicleTravelTime){
        return Math.max(0, Math.min(Parameters.maxJourneyDuration, firstVehicleTravelTime)
                - Math.min(Parameters.maxJourneyDuration, secondVehicleTravelTime));

    }
}

