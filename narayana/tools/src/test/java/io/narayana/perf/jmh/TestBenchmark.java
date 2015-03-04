package io.narayana.perf.jmh;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

public class TestBenchmark {
    private static final double LOWER_THRESHOLD = -20.0;
    private static final double UPPER_THRESHOLD = 20.0;
    private static final String RES_FMT = "com.hp.mwtests.ts.arjuna.performance.Performance1.twoPhase,thrpt,1,1,%f,NaN,ops/s";

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(Benchmark.PERFORMANCE_DECREASE_THRESHOLD_PROPERTY_NAME, String.valueOf(LOWER_THRESHOLD));
        System.setProperty(Benchmark.PERFORMANCE_INCREASE_THRESHOLD_PROPERTY_NAME, String.valueOf(UPPER_THRESHOLD));
    }

    @Test
    public void testSystemProperty() {
        Assert.assertEquals(Benchmark.PERFORMANCE_INCREASE_THRESHOLD, UPPER_THRESHOLD, 0.0001d);

    }
    @Test
    public void testRegression() throws IOException {
        double prevThrpt = 1000000;
        double currThrpt = prevThrpt * (LOWER_THRESHOLD + 99) / 100.0;

        compareBenchmarks(prevThrpt, currThrpt, -1);
    }

    @Test
    public void testNoRegression() throws IOException {
        double prevThrpt = 1000000;
        double currThrpt = prevThrpt * (LOWER_THRESHOLD + 101) / 100.0;

        compareBenchmarks(prevThrpt, currThrpt, 0);
    }

    @Test
    public void testNoImprovement() throws IOException {
        double prevThrpt = 1000000;
        double currThrpt = prevThrpt * (UPPER_THRESHOLD + 99) / 100.0;

        compareBenchmarks(prevThrpt, currThrpt, 0);
    }

    @Test
    public void testImprovement() throws IOException {
        double prevThrpt = 1000000;
        double currThrpt = prevThrpt * (UPPER_THRESHOLD + 101) / 100.0;

        compareBenchmarks(prevThrpt, currThrpt, 1);
    }

    private void compareBenchmarks(double prevThrpt, double currThrpt, int expect) throws IOException {
        String prevRes = String.format(RES_FMT, prevThrpt);
        String currRes = String.format(RES_FMT, currThrpt);

        BMParser bmp = new BMParser();
        Benchmark prev = bmp.parseBenchmark(prevRes);
        Benchmark curr = bmp.parseBenchmark(currRes);

        curr.setPrevious(prev);

        if (expect == -1)
            Assert.assertTrue(curr.isRegression());

        int res = curr.compareTo(prev);

        Assert.assertEquals(expect, res);
    }
}
