package Master2020.Genetic;
import Master2020.DataFiles.*;

public class PenaltyControl {

    Data data;
    double iteration;
    double numOverLoadFeasible;
    double numTimeWarpFeasible;
    double indifferenceValue = 0.05;
    double increase = 1.25;
    double decrease = 0.8;
    boolean verbose = false;
    private int updateFrequency;

    public double timeWarpPenalty;
    public double overLoadPenalty;


    public PenaltyControl(double timeWarpPenalty, double overLoadPenalty, int updateFrequency){
        this.timeWarpPenalty = timeWarpPenalty;
        this.overLoadPenalty = overLoadPenalty;
        this.updateFrequency = updateFrequency;
        reset();
    }

    public void adjust(boolean hasTimeWarp, boolean hasOverLoad) {
        iteration += 1;
        numTimeWarpFeasible += hasTimeWarp ? 0 : 1;
        numOverLoadFeasible += hasOverLoad ? 0 : 1;
        if (iteration >= updateFrequency) {
            updatePenalties();
            reset();
        }
    }


    public void updatePenalties() {
        updateTimeWarpPenalties();
        updateOverLoadPenalties();

    }

    public void updateTimeWarpPenalties(){
        double feasibleFraction = numTimeWarpFeasible/ iteration;
        if (verbose)
            System.out.println("Time warp feasible fraction: " + feasibleFraction);
        if (feasibleFraction > Parameters.fractionOfFeasibleIndividualsFromAdsplit + indifferenceValue){
            this.timeWarpPenalty *= decrease;
            if (verbose)
                System.out.println("Decrease timewarp: " + this.timeWarpPenalty);
        }
        else if( feasibleFraction < Parameters.fractionOfFeasibleIndividualsFromAdsplit - indifferenceValue){
            this.timeWarpPenalty *= increase;
            if (verbose)
                System.out.println("Increase timewarp: " + this.timeWarpPenalty);
        }
        else {
            if (verbose)
                System.out.println("No change");
        }

    }

    public void updateOverLoadPenalties(){
        double feasibleFraction = numOverLoadFeasible / iteration;
        if (verbose)
            System.out.println("Over load feasible fraction: " +feasibleFraction);
        if (feasibleFraction > Parameters.fractionOfFeasibleIndividualsFromAdsplit + indifferenceValue){
            this.overLoadPenalty *= decrease;
            if (verbose)
                System.out.println("Decrease overload: " + this.overLoadPenalty );
        }
        else if (feasibleFraction < Parameters.fractionOfFeasibleIndividualsFromAdsplit - indifferenceValue){
            this.overLoadPenalty *= increase;
            if (verbose)
                System.out.println("Increase overload: " + this.overLoadPenalty);

        }
        else {
            if (verbose)
                System.out.println("No change");
        }
    }

    public void reset(){
        iteration = 0;
        numOverLoadFeasible = 0;
        numTimeWarpFeasible = 0;

    }


}
