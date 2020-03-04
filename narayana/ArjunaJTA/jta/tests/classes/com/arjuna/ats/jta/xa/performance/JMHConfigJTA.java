/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package com.arjuna.ats.jta.xa.performance;

import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.runner.options.CommandLineOptionException;
import org.openjdk.jmh.runner.options.CommandLineOptions;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class JMHConfigJTA {

    public static final int WI = 1; //  -wi <int>                   Number of warmup iterations to do.
    public static final int WT = 1; //  -w <time>                   Time to spend at each warmup iteration.
    public static final TimeUnit WTU = TimeUnit.SECONDS;
    public static final TimeValue WTV = TimeValue.seconds(WT);

    public static final int MI = 2; //  -i <int>                    Number of measurement iterations to do.
    public static final int MT = 10; // -r <time>                   Time to spend at each measurement iteration.
    public static final TimeUnit MTU = TimeUnit.SECONDS;
    public static final TimeValue MTV = TimeValue.seconds(MT);

    public static final int BF = 1; //  -f [int]                    How many times to forks a single benchmark.
    public static final int BT = 1; //  -t <int>                    Number of worker threads to run with.

    public static final Runner getJTARunner(String[] args) throws CommandLineOptionException {

        for (String arg : args)
            if (arg.startsWith("-"))
                return new Runner(new CommandLineOptions(args));

        return new Runner(new OptionsBuilder()
                .include(".*")
                .warmupIterations(WI)
                .warmupTime(WTV)
                .measurementIterations(MI)
                .measurementTime(MTV)
                .forks(BF)
                .threads(BT)
                .build()
        );
    }

    public static void runJTABenchmark(String name, String[] args) throws RunnerException, CommandLineOptionException {
        Collection<RunResult> results = getJTARunner(args).run();

        for (RunResult runResult : results) {
            Result result = runResult.getPrimaryResult();

            System.out.printf("%nJTA (%s) benchmark score: %d %s over %d iterations%n",
                    name, result.getScore(), result.getScoreUnit(), result.getStatistics().getN());
        }
    }
}
