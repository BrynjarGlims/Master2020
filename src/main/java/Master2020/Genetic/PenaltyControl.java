package Master2020.Genetic;
import Master2020.DataFiles.*;

public class PenaltyControl {

    Data data;
    int iteration;
    int numOverload;
    int numTimeWarp;
    double indifferenceValue = 0.05;
    double increase = 1.25;
    double decrease = 0.8;


    public PenaltyControl(Data data){
        this.data = data;
        reset();

    }

    public void adjust(boolean hasTimeWarp, boolean hasOverLoad) {
        iteration += 1;
        numTimeWarp += hasTimeWarp ? 1 : 0;
        numOverload += hasOverLoad ? 1 : 0;
        if (iteration >= Parameters.frequencyOfPenaltyUpdates) {
            updatePenalties();
            reset();
        }
    }


    public void updatePenalties() {
        updateTimeWarpPenalties();
        updateOverLoadPenalties();

    }

    public void updateTimeWarpPenalties(){
        if (numTimeWarp/iteration > Parameters.fractionOfFeasibleIndividualsFromAdsplit + indifferenceValue){
            Parameters.initialTimeWarpPenalty *= increase;
        }
        else if(numTimeWarp/iteration < Parameters.fractionOfFeasibleIndividualsFromAdsplit - indifferenceValue){
            Parameters.initialTimeWarpPenalty *= decrease;
        }
    }

    public void updateOverLoadPenalties(){
        if (numOverload/iteration > Parameters.fractionOfFeasibleIndividualsFromAdsplit + indifferenceValue){
            Parameters.initialCapacityPenalty *= increase;
        }
        else if(numOverload/iteration < Parameters.fractionOfFeasibleIndividualsFromAdsplit - indifferenceValue){
            Parameters.initialCapacityPenalty *= decrease;
        }
    }

    public void reset(){
        iteration = 0;
        numOverload = 0;
        numTimeWarp = 0;

    }


}
