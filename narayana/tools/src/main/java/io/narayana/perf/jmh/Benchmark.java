/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package io.narayana.perf.jmh;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class Benchmark implements Comparable<Benchmark> {
    public static final String PERFORMANCE_INCREASE_THRESHOLD_PROPERTY_NAME = "PERFORMANCE_INCREASE_THRESHOLD";
    static final String PERFORMANCE_DECREASE_THRESHOLD_PROPERTY_NAME = "PERFORMANCE_DECREASE_THRESHOLD";

    public static final double PERFORMANCE_INCREASE_THRESHOLD =
            Double.parseDouble(System.getProperty(PERFORMANCE_INCREASE_THRESHOLD_PROPERTY_NAME, "10"));
    static final double PERFORMANCE_DEGRADATION_THRESHOLD =
            Double.parseDouble(System.getProperty(PERFORMANCE_DECREASE_THRESHOLD_PROPERTY_NAME, "-10"));
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
        if (previous != null) {
            double pc = ( score / previous.score - 1) * 100.0;
            String res = regression == -1 ? "regression" : regression == 0 ? "no change" : "improvement";
            return String.format("%s: %f vrs %f (%f%%: %s)%n",
                    benchmark, score, previous.score, pc, res);
        } else {
            return String.format("%s: %f%n", benchmark, score);
        }
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
 
        System.out.printf("Comparison (pull request versus master)%n");
        System.out.printf("(changes within the %% range [%f, %f] are regarded as insignificant):%n%n",
            PERFORMANCE_DEGRADATION_THRESHOLD, PERFORMANCE_INCREASE_THRESHOLD);

        for (Benchmark bm : nbms.values()) {
            System.out.printf("%s", bm.toString());
            if (bm.isRegression())
                regression = true;
        }

        if (regression)
            System.exit(1);
    }
}
