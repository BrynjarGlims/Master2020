package Master2020.Utils;


import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class WeightedRandomSampler {

    private double[] weights;
    private ThreadLocalRandom random = ThreadLocalRandom.current();

    public WeightedRandomSampler(double[] weights) {
        double[] accumulated = {0};
        this.weights = Arrays.stream(weights).map(d -> {
            accumulated[0] += d;
            return accumulated[0];
        }).toArray();
    }

    public int nextIndex() {
        double rand = random.nextDouble(weights[weights.length - 1]);
        int i = 0;
        while (rand > weights[i]) i++;
        return i;
    }

    public static void main(String[] args) {
        double[] weights = new double[]{20, 10, 40, 30};
        WeightedRandomSampler ws = new WeightedRandomSampler(weights);
        int[] counts = new int[4];
        for (int i = 0; i < 1000000; i++) {
            counts[ws.nextIndex()] += 1;
        }
        System.out.println(Arrays.toString(counts));
    }
}



