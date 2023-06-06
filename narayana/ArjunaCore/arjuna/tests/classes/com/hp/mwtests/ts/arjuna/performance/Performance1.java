/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */



package com.hp.mwtests.ts.arjuna.performance;

import com.arjuna.ats.arjuna.AtomicAction;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.internal.arjuna.objectstore.VolatileStore;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;
import com.hp.mwtests.ts.arjuna.JMHConfigCore;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.CommandLineOptionException;

//@Warmup(iterations = JMHConfigCore.WI, time = JMHConfigCore.WT)
// , timeUnit = JMHConfigCore.WTU)
//@Measurement(iterations = JMHConfigCore.MI, time = JMHConfigCore.MT)
// , timeUnit = JMHConfigCore.MTU)
//@Fork(JMHConfigCore.BF)
//@Threads(JMHConfigCore.BT)
public class Performance1 {
    @State(Scope.Thread)
    public static class BenchmarkState {
        private BasicRecord record1 = new BasicRecord();
        private BasicRecord record2 = new BasicRecord();
    }

    static {
        try {
            BeanPopulator.getDefaultInstance(ObjectStoreEnvironmentBean.class).setObjectStoreType(VolatileStore.class.getName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    public boolean onePhaseTest(BenchmarkState benchmarkState, Blackhole bh) {
        bh.consume(onePhase(benchmarkState));

        return true;
    }

    private boolean onePhase(BenchmarkState benchmarkState) {
        AtomicAction A = new AtomicAction();

        A.begin();
        A.add(benchmarkState.record1);
        A.commit();
        return true;
    }

    @Benchmark
    public boolean twoPhaseTest(BenchmarkState benchmarkState, Blackhole bh) {
        bh.consume(twoPhase(benchmarkState));

        return true;
    }

    private boolean twoPhase(BenchmarkState benchmarkState) {
        AtomicAction A = new AtomicAction();

        A.begin();
        A.add(benchmarkState.record1);
        A.add(benchmarkState.record2);
        A.commit();
        return true;
    }

    public static void main(String[] args) throws RunnerException, CommandLineOptionException {
        JMHConfigCore.runJTABenchmark(Performance1.class.getSimpleName(), args);
    }
}