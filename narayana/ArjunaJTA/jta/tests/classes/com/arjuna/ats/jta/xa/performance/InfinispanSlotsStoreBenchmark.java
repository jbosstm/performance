/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package com.arjuna.ats.jta.xa.performance;

import com.arjuna.ats.internal.arjuna.objectstore.slot.SlotStoreAdaptor;
import com.arjuna.ats.internal.arjuna.objectstore.slot.infinispan.InfinispanSlots;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import org.infinispan.configuration.cache.CacheMode;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.JavaFlightRecorderProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class InfinispanSlotsStoreBenchmark extends InfinispanSlotsStoreBase {
    // the name of the cluster and the shared cache used for the object store
    static final String CLUSTER_NAME = "objectStoreCluster";

    static Store store;
    static final int THREADS = 240;//10;//_000; //Threads.MAX;
    static final int FORKS = 1;
    static final int ITERATIONS = 5;
    static final int TIME_PER_ITER = 2;

    public static void main(String[] args) throws RunnerException {
        // Sometimes it is useful to run the benchmark directly from an IDE:
        Options opt = new OptionsBuilder()
                .include(InfinispanSlotsStoreBenchmark.class.getSimpleName() + ".testWriteThroughCache")

                .timeUnit(TimeUnit.SECONDS)
                .threads(THREADS)
                .forks(FORKS)
                .mode(Mode.Throughput)
                .warmupIterations(1)
                .warmupTime(TimeValue.seconds(1))
                .measurementIterations(ITERATIONS)
                .measurementTime(TimeValue.seconds(TIME_PER_ITER))
                .shouldDoGC(true)
                // use JFR as the profiler, the recording will appear in the User working directory with the
                // name "<package name>-xxx/profile.jfr", which you can change to "wherever" using
                // addProfiler(JavaFlightRecorderProfiler.class, "dir=wherever"). Java Flight Recorder data files
                // can be viewed with the jmc graphical tool or with the jfr command line tool which is in the java
                // bin directory
                .addProfiler(JavaFlightRecorderProfiler.class)
                .jvmArgs("-Djmh.executor=FJP") // ForkJoinPool
                .build();

        new Runner(opt).run();
    }


    @Setup(Level.Trial)
    public static void setup() throws Throwable {
        store = getStore("node1", CacheMode.REPL_SYNC, 3, null, true, false, null);
        store.config().setBackingSlotsClassName(InfinispanSlots.class.getName());
        replaceEnvironmentBean(store.config());
        store.start();

        JTAStoreBase.setup(SlotStoreAdaptor.class.getName());
    }

    @TearDown
    public static void tearDown() {
        store.stop();
    }

    @Benchmark
    public void testInfinispanStore(Blackhole bh) throws HeuristicRollbackException, SystemException, HeuristicMixedException, NotSupportedException, RollbackException {
        bh.consume(super.jtaTest());
    }
}