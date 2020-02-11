package Individual;

import DataFiles.Parameters;

import java.util.ArrayList;

public class LabelPool {

    ArrayList<Label> labels;

    public LabelPool (){

        
    }


    public LabelPool generateLabels(ArrayList<Label> previousLabels, double arcCost){
        for (Label labels : previousLabels){


        }
        return new LabelPool();


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

