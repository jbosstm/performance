package com.hp.mwtests.ts.arjuna;

import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.CommandLineOptionException;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.runner.options.CommandLineOptions;

import java.util.Collection;

public class BenchmarkRunner {
    static Runner getRunner(Options defaultOptions, String[] args) throws CommandLineOptionException {

        for (String arg : args)
            if (arg.startsWith("-"))
                return new Runner(new CommandLineOptions(args));

        return new Runner(defaultOptions);
    }

    public static void main(String[] args) throws RunnerException, CommandLineOptionException {
        Options opts = new OptionsBuilder()
                .include(com.hp.mwtests.ts.arjuna.atomicaction.CheckedActionTest.class.getSimpleName())
                .warmupIterations(1)
                .measurementIterations(1)
                .threads(1)
                .forks(1)
                .measurementTime(TimeValue.seconds(10))
                .build();

        Collection<RunResult> results = new Runner(opts).run();
//        getRunner(opts, args).run();

        for (RunResult runResult : results) {
            Result result = runResult.getPrimaryResult();

            System.out.printf("%nbenchmark score: %d %s over %d iterations%n",
                    result.getScore(), result.getScoreUnit(), result.getStatistics().getN());
        }
    }
}
