/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package com.arjuna.ats.jta.xa.performance;

import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.objectstore.StoreManager;
import com.arjuna.ats.internal.arjuna.objectstore.slot.SlotStoreAdaptor;
import com.arjuna.ats.internal.arjuna.objectstore.slot.SlotStoreEnvironmentBean;
import com.arjuna.ats.internal.arjuna.objectstore.slot.jgroups.JGroupsSlots;
import com.arjuna.ats.internal.arjuna.objectstore.slot.jgroups.JGroupsStoreEnvironmentBean;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import org.junit.BeforeClass;
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

import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * JMH Benchmark for the JGroupsSlots store (with a persistent write-ahead log).
 * <p>The benchmark uses a single-node cluster for performance testing
 * <p>Run from IDE: Run main() method
 * <p>Run from command line: See performance/README.md
 */
@State(Scope.Benchmark)
public class JGroupsSlotsStoreBenchmark extends JTAStoreBase {
    static final int THREADS = 240;
    static final String BM_CLASS_NAME = JGroupsSlotsStoreBenchmark.class.getSimpleName();

    static final int FORKS = 1;
    static final int ITERATIONS = 5;
    static final int TIME_PER_ITER = 2;

    public static void main(String[] args) throws RunnerException {
        // Sometimes it is useful to run the benchmark directly from an IDE:
        Options opt = new OptionsBuilder()
                .include(BM_CLASS_NAME + ".testJGroupsSlotsStore")
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
        JGroupsStoreEnvironmentBean configBean = BeanPopulator.getDefaultInstance(JGroupsStoreEnvironmentBean.class);
        configBean.setExperimentalEnabled(true);
        SlotStoreEnvironmentBean slotStoreBean = BeanPopulator.getDefaultInstance(SlotStoreEnvironmentBean.class);
        slotStoreBean.setBackingSlotsClassName(JGroupsSlots.class.getName());
        int threadCount = getThreadCountFromProperties(THREADS);

        // JGroups configuration
        configBean.setJGroupsConfigFileName("jgroups.xml");
        configBean.setNodeAddress("benchmark-node");
        configBean.setClusterName("jgroups-slots-benchmark-" + System.currentTimeMillis());
        configBean.setReplicationCount((short) -1); // Full replication

        // write-ahead log configuration
        configBean.setWalEnabled(true);  // set to true for durability of transaction logs
        configBean.setWalSyncWrites(true); // set to true for maximum durability
        configBean.setWalSyncDeletes(false);

        configBean.setRecycleFailedSlots(true); // otherwise failed slot writes will lead to slot exhaustion

        configBean.setWalAsyncIO(false); // requires libaio to be on the LD_LIBRARY_PATH

        // L2 cache configuration (disabled for consistency)
        configBean.setCachingTime(0L);

        // SlotStoreAdaptor creates the SlotStore using the base SlotStoreEnvironmentBean,
        // so numberOfSlots must be set there (not just on the JGroups config bean)
        int numberOfSlots = roundUp(256, threadCount);
        slotStoreBean.setNumberOfSlots(numberOfSlots);
        configBean.setNumberOfSlots(numberOfSlots);
        configBean.setBackingSlotsClassName(JGroupsSlots.class.getName());

        cleanStore(Paths.get(configBean.getStoreDir()).toFile());
    }

    @TearDown(Level.Trial)
    public static void tearDown() {
        StoreManager.shutdown();
        JGroupsStoreEnvironmentBean configBean = BeanPopulator.getDefaultInstance(JGroupsStoreEnvironmentBean.class);
        cleanStore(Paths.get(configBean.getStoreDir()).toFile());
    }

    @Benchmark
    public void testJGroupsSlotsStore(Blackhole bh) throws HeuristicRollbackException, SystemException, HeuristicMixedException, NotSupportedException, RollbackException {
        bh.consume(super.jtaTest());
    }
}
