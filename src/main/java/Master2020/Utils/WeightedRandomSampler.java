package Master2020.Utils;


import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class WeightedRandomSampler {

    public double[] weights;
    private double increment;
    private ThreadLocalRandom random = ThreadLocalRandom.current();

    public WeightedRandomSampler(double[] weights) {
        double[] accumulated = {0};
        this.weights = Arrays.stream(weights).map(d -> {
            accumulated[0] += d;
            return accumulated[0];
        }).toArray();
        increment = 1.0/(weights.length -1);
    }

    public int nextIndex() {
        double rand = random.nextDouble(weights[weights.length - 1]);
        int i = 0;
        while (rand > weights[i]) i++;
        return i;
    }


    public static void main(String[] args) {
        double[] weights = new double[]{20, 10, 20, 30, 15, 5};
        WeightedRandomSampler ws = new WeightedRandomSampler(weights);

    }
}



