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
