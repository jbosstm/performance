/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package com.arjuna.ats.jta.xa.performance;

import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import org.infinispan.commons.marshall.WrappedByteArray;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.conflict.MergePolicy;
import org.infinispan.distribution.group.Grouper;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.partitionhandling.PartitionHandling;
import org.junit.Assert;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.CommandLineOptionException;

import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

// this benchmark verifies that the write through caching feature of infinispan is operational
// the feature is needed for a fair comparison against other object store types
@State(Scope.Benchmark)
public class InfinispanSlotsRestartStoreBenchmark extends InfinispanSlotsStoreBase {
    // the name of the cluster and the shared cache used for the object store
    static final String CLUSTER_NAME = "objectStoreCluster";

    public static void main(String[] args) throws RunnerException, CommandLineOptionException, CoreEnvironmentBeanException {
        JMHConfigJTA.runJTABenchmark(InfinispanSlotsRestartStoreBenchmark.class.getSimpleName(), args);
    }

    static DefaultCacheManager createCacheManager(String nodeName,
                                                  CacheMode cacheMode,
                                                  int numOwners, // used with CacheMode.CacheMode
                                                  Grouper<WrappedByteArray> grouper,
                                                  boolean persistence,
                                                  boolean partitionResilience) {
        GlobalConfigurationBuilder globalConfig = GlobalConfigurationBuilder.defaultClusteredBuilder();

        globalConfig.transport().nodeName(nodeName).machineId(nodeName)
                .clusterName(CLUSTER_NAME);//.addProperty("configurationFile", "jgroups.xml");

        var manager = new DefaultCacheManager(globalConfig.build());
        var storeDir = String.format("%s/%s", STORE_DIR, nodeName);

        // Define the replicated cache configuration
        ConfigurationBuilder cacheConfig = new ConfigurationBuilder();
        cacheConfig
                .clustering()
                .cacheMode(cacheMode)
                .remoteTimeout(5, TimeUnit.SECONDS);

        if (numOwners > 0) {
            cacheConfig
                    .clustering()
                    .hash()
                    .numOwners(numOwners); // number of cluster-wide replicas for each cache entry
        }
        if (grouper != null) {
            cacheConfig
                    .clustering()
                    .hash().groups().enabled().addGrouper(grouper);
        }
        if (persistence) {
            cacheConfig
                    .clustering()
                    .persistence()
                    .passivation(false)
                    .addSoftIndexFileStore()
                    .dataLocation(storeDir + "/data")
                    .indexLocation(storeDir + "/index")
                    .shared(false);
        }
        if (partitionResilience) {
            cacheConfig
                    .clustering()
                    .partitionHandling()
                    .whenSplit(PartitionHandling.DENY_READ_WRITES)
                    .mergePolicy(MergePolicy.PREFERRED_ALWAYS);
        }

        manager.defineConfiguration(CLUSTER_NAME, cacheConfig.build());

        return manager;
    }

    @State(Scope.Benchmark)
    public static class SlotStoreBenchmarkState {
        Store store1;
        Store store2;

        @Setup(Level.Trial)
        public void doSetup() {
            store1 = new Store(createCacheManager("node1", CacheMode.REPL_SYNC, -1, null, true, false), null, "node1");
            store2 = new Store(createCacheManager("node2", CacheMode.REPL_SYNC, -1, null, true, false), null, "node2");

            store1.manager().getCache(CLUSTER_NAME).clear(); // start clean
            store2.manager().getCache(CLUSTER_NAME).clear(); // start clean
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            try {
                store1.slots().clear(0, true);
                store2.slots().clear(1, true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            store1.stop();
            store2.stop();
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Threads(1)
    @Fork(1) // run the benchmark in two processes/JVMs
    @Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 1, time = 1, timeUnit = TimeUnit.MILLISECONDS)
    public void validateWriteThroughCacheIsOperational(SlotStoreBenchmarkState state) throws IOException {

        // create two key value pairs
        record KVPair(byte[] key, byte[] value) {}
        KVPair kv1 = new KVPair("key1".getBytes(), "value1".getBytes());
        KVPair kv2 = new KVPair("key2".getBytes(), "value2".getBytes());

        // populate the first infinispan cache with them
        state.store1.cache().put(kv1.key, kv1.value);
        state.store1.cache().put(kv2.key, kv2.value);

        // make sure to start the store after updating the infinispan caches, then starting the store will
        // load the keys just were added.
        state.store1.start();
        state.store2.start();

        // and check that the infinispan filesystem persistence store was created at both nodes
        Assert.assertTrue("infinispan persistence store 1 was not created: " + state.store1.path(), Files.exists(state.store1.path()));
        Assert.assertTrue("infinispan persistence store 2 was not created: " + state.store2.path(), Files.exists(state.store2.path()));

        // verify that the slot stores at each node have the same values
        // note that the slots backend is internal, but it's still useful to test it directly
        byte[] value1 = state.store1.slots().read(0);
        byte[] value2 = state.store2.slots().read(0);
        byte[] value3 = state.store1.slots().read(1);
        byte[] value4 = state.store2.slots().read(1);

        Assert.assertArrayEquals(value1, value2);
        Assert.assertArrayEquals(value3, value4);

        // stop both nodes
        state.store1.stop();
        state.store2.stop();

        /*
         * All nodes are now down so we know none of the data can be in-memory.
         * Restart them and verify that they repopulated their caches
         * from the filesystem backing stores on their respective nodes
         */
        state.store1 = new InfinispanSlotsRestartStoreBenchmark.Store(createCacheManager("node1", CacheMode.REPL_SYNC, -1, null, true, false), null, "node1");
        state.store2 = new InfinispanSlotsRestartStoreBenchmark.Store(createCacheManager("node2", CacheMode.REPL_SYNC, -1, null, true, false), null, "node2");

        state.store1.start();
        state.store2.start();

        Assert.assertArrayEquals(kv1.value, state.store1.cache().get(kv1.key));
        Assert.assertArrayEquals(kv1.value, state.store2.cache().get(kv1.key));
        Assert.assertArrayEquals(kv2.value, state.store1.cache().get(kv2.key));
        Assert.assertArrayEquals(kv2.value, state.store2.cache().get(kv2.key));

        // and similarly check that the slot stores are repopulated correctly
        Assert.assertArrayEquals(state.store1.slots().read(0), state.store2.slots().read(0));
        Assert.assertArrayEquals(state.store1.slots().read(1), state.store2.slots().read(1));
    }
}