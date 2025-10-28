/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */
package org.jboss.narayana.stm;

import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;
import com.arjuna.ats.arjuna.AtomicAction;

import org.jboss.stm.Container;
import org.jboss.stm.annotations.Optimistic;
import org.jboss.stm.annotations.ReadLock;
import org.jboss.stm.annotations.Transactional;
import org.jboss.stm.annotations.WriteLock;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.Warmup;

import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;

public class STMBenchmark {
    @Transactional
    @Optimistic
    public interface Sample {
        void increment();
        void decrement();
        int value();
    }

    public static class SampleLockable implements Sample {
        public SampleLockable(int init) {
            _isState = init;
        }

        @ReadLock
        public int value() {
            return _isState;
        }

        @WriteLock
        public void increment() {
            _isState++;
        }

        @WriteLock
        public void decrement() {
            _isState--;
        }

        @org.jboss.stm.annotations.State
        private int _isState;
    }

    @org.openjdk.jmh.annotations.State(Scope.Benchmark)
    public static class BenchmarkState {
        Container<Sample> theContainer;
        Sample obj1;
        Sample obj2;
        SplittableRandom rand;
        int a;
        int b;

        @Setup(Level.Iteration)
        public void doSetup() {
            BeanPopulator.getDefaultInstance(ObjectStoreEnvironmentBean.class).setObjectStoreDir("/tmp/stm-benchmark-store");
            rand = new SplittableRandom(42);
            theContainer = new Container<>();
            obj1 = theContainer.create(new SampleLockable(10));
            obj2 = theContainer.create(new SampleLockable(10));
            // not thread safe, reproducible and fast enough (fingers crossed!)
            a = 1;
            b = 2;
        }
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Fork(2) // run the benchmark in two processes/JVMs
    @Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS) // 10 seconds in total
    @Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS) // 10 seconds in total
    public void benchmark(BenchmarkState state) {
        AtomicAction A = new AtomicAction();
        boolean doCommit = true;

        A.begin();

        try {
            // always keep the two objects in sync.

            state.obj1.increment();
            state.obj2.decrement();
        } catch (final Throwable ex) {
            doCommit = false;
        }

        if (state.rand.nextInt() % 2 == 0)
            doCommit = false;

        if (doCommit) {
            A.commit();
        } else {
            A.abort();
        }
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Fork(2)
    @Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
    public int baseLineBenchmark(BenchmarkState state) {
        return state.a + state.b;
    }
}
