package org.jboss.narayana.stm;

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
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
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
        Random rand;

        @Setup(Level.Invocation)
        public void doSetup() {
            theContainer = new Container<Sample>();
            obj1 = theContainer.create(new SampleLockable(10));
            obj2 = theContainer.create(new SampleLockable(10));
            rand = new Random();
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
    public void baseLineBenchmark(Blackhole blackhole) {
        int a = 1;
        int b = 2;
        int sum = a + b;
        blackhole.consume(sum);
    }
}