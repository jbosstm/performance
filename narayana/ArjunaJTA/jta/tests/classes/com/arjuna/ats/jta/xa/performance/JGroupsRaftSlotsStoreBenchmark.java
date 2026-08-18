/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package com.arjuna.ats.jta.xa.performance;

import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.objectstore.StoreManager;
import com.arjuna.ats.internal.arjuna.objectstore.slot.SlotStoreAdaptor;
import com.arjuna.ats.internal.arjuna.objectstore.slot.SlotStoreEnvironmentBean;
import com.arjuna.ats.internal.arjuna.objectstore.slot.jgroups.JGroupsRaftSlots;
import com.arjuna.ats.internal.arjuna.objectstore.slot.jgroups.JGroupsRaftStoreEnvironmentBean;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import org.junit.BeforeClass;
import org.openjdk.jmh.annotations.AuxCounters;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.infra.Control;
import org.openjdk.jmh.profile.JavaFlightRecorderProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * JMH Benchmark for the JGroupsRaftSlots store (Raft consensus with a persistent write-ahead log).
 * <p>The benchmark uses a single-node Raft cluster for performance testing (in production, Raft requires 3 or more nodes)
 * <p>Run from IDE: Run main() method
 * <p>Run from command line: See performance/README.md e.g.
 * <pre>
 *   java -jar narayana/ArjunaJTA/jta/target/benchmarks.jar \
 *     "JGroupsRaftSlotsStoreBenchmark|JGroupsSlotsStoreBenchmark|HQStoreBenchmark|ShadowNoFileLockStoreBenchmark" \
 *     -t 10 -f 1 -wi 3 -i 5 -r 10
 * </pre>
 * <p>which produces:
 *   3 warmup iterations (lets JIT stabilize), 5 measurement iterations (more data points), 10-second windows (I/O stalls average out).
 * <p>It takes a bit longer (~4 minutes on a typical development machine) but the error margins drop to
 *    single-digit percentages of the score.
 * <p>The quickest run, for these fsync-heavy benchmarks, that still produces tolerable variance is -wi 2 -i 5 -r 5
 */
@State(Scope.Benchmark)
public class JGroupsRaftSlotsStoreBenchmark extends JTAStoreBase {
    static final int THREADS = 240;
    static final String BM_CLASS_NAME = JGroupsRaftSlotsStoreBenchmark.class.getSimpleName();

    static final int FORKS = 1;
    static final int ITERATIONS = 5;
    static final int TIME_PER_ITER = 2;

    public static void main(String[] args) throws RunnerException {
        // Sometimes it is useful to run the benchmark directly from an IDE:
        Options opt = new OptionsBuilder()
                .include(BM_CLASS_NAME + ".testJGroupsRaftSlotsStore")
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
                // to debug the forks use "-agentlib:jdwp=transport=dt_socket,address=5005,server=y,suspend=y"
                .build();

        new Runner(opt).run();
    }

    @Setup(Level.Trial)
    @BeforeClass
    public static void setup() throws CoreEnvironmentBeanException {
        JTAStoreBase.setup(SlotStoreAdaptor.class.getName());
        JGroupsRaftStoreEnvironmentBean configBean = BeanPopulator.getDefaultInstance(JGroupsRaftStoreEnvironmentBean.class);
        configBean.setExperimentalEnabled(true);
        SlotStoreEnvironmentBean slotStoreBean = BeanPopulator.getDefaultInstance(SlotStoreEnvironmentBean.class);
        slotStoreBean.setBackingSlotsClassName(JGroupsRaftSlots.class.getName());
        int threadCount = getThreadCountFromProperties(THREADS);
        // Raft serializes all writes through a single consensus thread, so very high thread
        // counts produce slot exhaustion rather than meaningful throughput data.
        // Cap the slot allocation to THREADS to keep the benchmark stable at all -t (no of worker threads) values.
        int effectiveThreadCount = Math.min(threadCount, THREADS);

        // JGroups configuration
        configBean.setJGroupsConfigFileName("jgroups-raft.xml");
        configBean.setNodeAddress("raft-benchmark-node"); // TODO configure 3 node cluster
        configBean.setClusterName("jgroups-raft-benchmark-" + System.currentTimeMillis());

        // Note: Using default key generator (Uid-based) for single-node benchmark

        // Raft configuration (single-node cluster for benchmark)
        configBean.setRaftMembers("raft-benchmark-node"); // Single node
        configBean.setRaftLogFsync(true);  // durable writes for fault tolerance
        configBean.setAllowDirtyReads(false);

        configBean.setRecycleFailedSlots(true); // otherwise failed slot writes will lead to slot exhaustion

        // SlotStoreAdaptor creates the SlotStore using the base SlotStoreEnvironmentBean,
        // so numberOfSlots must be set there (not just on the JGroups config bean)
        int numberOfSlots = roundUp(256, effectiveThreadCount);
        slotStoreBean.setNumberOfSlots(numberOfSlots);
        configBean.setNumberOfSlots(numberOfSlots);
        configBean.setBackingSlotsClassName(JGroupsRaftSlots.class.getName());

        cleanStore(Paths.get(configBean.getStoreDir()).toFile());
    }

    @TearDown(Level.Trial)
    public static void tearDown() {
        StoreManager.shutdown();
        JGroupsRaftStoreEnvironmentBean configBean = BeanPopulator.getDefaultInstance(JGroupsRaftStoreEnvironmentBean.class);
        cleanStore(Paths.get(configBean.getStoreDir()).toFile());
    }

    @AuxCounters(AuxCounters.Type.OPERATIONS)
    @State(Scope.Thread)
    /*
     TxnCounters is a JMH @AuxCounters class that breaks the benchmark's throughput into two separate metrics in the JMH output:
     - committed — incremented when jtaTest() succeeds (transaction committed)
     - rolledBack — incremented when jtaTest() throws RollbackException (slot exhaustion under Raft contention)

     JMH treats both fields as operation counts and reports them as separate ops/sec columns alongside the main throughput metric.
     This lets you see at a glance how much of the reported throughput is real committed work versus failed transactions
     (important for Raft at high thread counts where many transactions roll back due to slot exhaustion).

     AuxCounters are approximate due to warmup/warmdown inclusion, the committed/rolledBack ratio is still meaningful
     even if the absolute ops/s values overshoot. But we can do better: use JMH Control object passed into the benchmark
     to determine if the iteration has started/stopped:- "Control is an infrastructure object passed into benchmark methods
     to check if an iteration has started or stopped (stopMeasurement)".

     An example from a run output with "-t 10 -f 2 -wi 3 -i 10 -r 10" shows no rolledBack counts
     (the Cnt column is the number of data points, forks (-f) x measurement iterations (-i)):

     Benchmark                                                             Mode  Cnt     Score     Error  Units
     JGroupsRaftSlotsStoreBenchmark.testJGroupsRaftSlotsStore             thrpt   20  1780.754 ± 276.787  ops/s
     JGroupsRaftSlotsStoreBenchmark.testJGroupsRaftSlotsStore:committed   thrpt   20  1782.559 ± 276.783  ops/s
     JGroupsRaftSlotsStoreBenchmark.testJGroupsRaftSlotsStore:rolledBack  thrpt   20       ≈ 0            ops/s

     Without @AuxCounters, catching RollbackException silently would inflate the throughput number (JMH counts every benchmark method invocation as one operation, whether it committed or not). With them, you get
     three columns: total ops/sec, committed ops/sec, and rolledBack ops/sec.
    */
    public static class TxnCounters {
        public long committed;
        public long rolledBack;
    }

    @Benchmark
    public void testJGroupsRaftSlotsStore(Blackhole bh, TxnCounters counters, Control control) throws HeuristicRollbackException, SystemException, HeuristicMixedException, NotSupportedException {
        try {
            bh.consume(super.jtaTest());
            if (control.startMeasurement && !control.stopMeasurement) {
                counters.committed++;
            }
        } catch (RollbackException e) {
            if (control.startMeasurement && !control.stopMeasurement) {
                counters.rolledBack++;
            }
        }
    }
}
