package Master2020.Genetic;
import Master2020.DataFiles.*;

public class PenaltyControl {

    Data data;
    double iteration;
    double numOverload;
    double numTimeWarp;
    double indifferenceValue = 0.05;
    double increase = 1.25;
    double decrease = 0.8;
    boolean verbose = false;

    public double timeWarpPenalty;
    public double overLoadPenalty;


    public PenaltyControl(double timeWarpPenalty, double overLoadPenalty){
        this.timeWarpPenalty = timeWarpPenalty;
        this.overLoadPenalty = overLoadPenalty;
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
        double feasibleFraction = 1 - (numTimeWarp/ iteration);
        if (verbose)
            System.out.println(feasibleFraction);
        if (feasibleFraction > Parameters.fractionOfFeasibleIndividualsFromAdsplit + indifferenceValue){
            this.timeWarpPenalty *= increase;
            if (verbose)
                System.out.println("Increase timewarp: " + this.timeWarpPenalty);
        }
        else if( feasibleFraction < Parameters.fractionOfFeasibleIndividualsFromAdsplit - indifferenceValue){
            this.timeWarpPenalty *= decrease;
            if (verbose)
                System.out.println("Decrease timewarp: " + this.timeWarpPenalty);
        }
        else {
            if (verbose)
                System.out.println("No change");
        }

    }

    public void updateOverLoadPenalties(){
        double feasibleFraction = 1 - (numOverload/ iteration);
        if (verbose)
            System.out.println(feasibleFraction);
        if (feasibleFraction > Parameters.fractionOfFeasibleIndividualsFromAdsplit + indifferenceValue){
            this.overLoadPenalty *= increase;
            if (verbose)
                System.out.println("Increase overload: " + this.overLoadPenalty);
        }
        else if (feasibleFraction < Parameters.fractionOfFeasibleIndividualsFromAdsplit - indifferenceValue){
            this.overLoadPenalty *= decrease;
            if (verbose)
                System.out.println("Decrease overload: " + this.overLoadPenalty );
        }
        else {
            if (verbose)
                System.out.println("No change");
        }
    }

    public void reset(){
        iteration = 0;
        numOverload = 0;
        numTimeWarp = 0;

    }


}
