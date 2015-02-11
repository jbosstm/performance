package io.narayana.perf.jmh;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class Benchmark implements Comparable<Benchmark> {
    static final double PERFORMANCE_INCREASE_THRESHOLD = 10;
    static final double PERFORMANCE_DEGRADATION_THRESHOLD = -10;
    static DecimalFormat DF = new DecimalFormat("#.##");

    String benchmark;
    String mode;
    Integer threads;
    Integer samples;
    Double score; // Mean
    Double scoreError; // meanError
    String unit;
    int regression = 0;

    private Benchmark previous;

    @Override
    public int compareTo(Benchmark other) {
        double change = ( score / other.score - 1) * 100.0;

        change = Double.valueOf(DF.format(change));

        if (change <= PERFORMANCE_DEGRADATION_THRESHOLD)
            return -1;

        if (change >= PERFORMANCE_INCREASE_THRESHOLD)
            return 1;

        return 0;
    }

    public void setPrevious(Benchmark previous) {
        this.previous = previous;

        regression = compareTo(previous);
    }

    @Override
    public String toString() {
        if (previous != null)
            return String.format("%s: %f vrs %f (change: %d)%n",
                    benchmark, score, previous.score, regression);
        else
            return String.format("%s: %f%n", benchmark, score);
    }

    public boolean isRegression() {
        return regression == -1;
    }

    public static void main(String[] args) throws IOException {
        Map<String, Benchmark> pbms = new HashMap<>();
        Map<String, Benchmark> nbms = new HashMap<>();
        boolean regression = false;

        for (String fname : args) {
            Map<String, Benchmark> bms = BMParser.readBenchmarks(fname);

            if (fname.endsWith("-master.csv"))
                pbms.putAll(bms);
            else if (fname.endsWith("-pr.csv"))
                nbms.putAll(bms);
            else
                System.err.printf("%s: don't know whether these are the new or the previous benchmarks", fname);
        }

        for (Benchmark bm : nbms.values())
            bm.setPrevious(pbms.get(bm.benchmark));

        for (Benchmark bm : nbms.values()) {
            System.out.printf("%s", bm.toString());
            if (bm.isRegression())
                regression = true;
        }

        if (regression)
            System.exit(1);
    }
}
