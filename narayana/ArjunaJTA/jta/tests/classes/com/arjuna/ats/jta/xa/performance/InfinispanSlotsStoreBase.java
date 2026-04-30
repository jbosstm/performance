/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package com.arjuna.ats.jta.xa.performance;

import com.arjuna.ats.internal.arjuna.objectstore.slot.SlotStoreEnvironmentBean;
import com.arjuna.ats.internal.arjuna.objectstore.slot.infinispan.InfinispanSlotKeyGenerator;
import com.arjuna.ats.internal.arjuna.objectstore.slot.infinispan.InfinispanSlots;
import com.arjuna.ats.internal.arjuna.objectstore.slot.infinispan.InfinispanStoreEnvironmentBean;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;
import org.infinispan.Cache;
import org.infinispan.commons.marshall.WrappedByteArray;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.conflict.MergePolicy;
import org.infinispan.distribution.group.Grouper;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.partitionhandling.PartitionHandling;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class InfinispanSlotsStoreBase extends JTAStoreBase {
    // the name of the cluster and the shared cache used for the object store
    static final String CLUSTER_NAME = "objectStoreCluster";
    // location of the file system store (with surefire it will be the build directory)
    static final String STORE_DIR = System.getProperty("narayana.storeLocation") + "/infinispan-caches";


    // record bringing together various data related to a slot store instance
    record Store(DefaultCacheManager manager, // the infinispan cache manager
                 String groupName,
                 String nodeName, // name of a cluster node
                 Cache<byte[], byte[]> cache, // the cache for this cluster node
                 InfinispanStoreEnvironmentBean config, // config for the slot store on this node
                 InfinispanSlots slots, // slot store
                 Path path) { // filesystem path where the persistent cache is located
        public Store(DefaultCacheManager manager, String groupName, String nodeName) {
            this(
                    manager,
                    groupName,
                    nodeName,
                    manager.getCache(CLUSTER_NAME),
                    new InfinispanStoreEnvironmentBean(),
                    new InfinispanSlots(),
                    Paths.get(STORE_DIR + "/" + nodeName)
            );
            config.setNodeAddress(manager.getNodeAddress());
            config.setGroupName(groupName);
            config.setCacheName(cache.getName());
            config.setCache(cache);
            config.setBackingSlots(slots);
            config.setSlotKeyGeneratorClassName(InfinispanSlotKeyGenerator.class.getName()); // default key generator
        }

        /*
         * Stop the cache manager otherwise the network endpoints won't be closed correctly.
         * This is particularly important with jgroups.
         */
        public void stop() {
            cache().stop();
            manager.stop();
        }

        public void start() throws IOException {
            slots().init(config());
        }
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

    static Store getStore(String nodeName, CacheMode mode, int numOwners, Grouper<WrappedByteArray> grouper, boolean persistence, boolean partitionResilience, String groupName) {
        DefaultCacheManager manager = createCacheManager(nodeName, mode, numOwners, grouper, persistence, partitionResilience);
        return new Store(manager, groupName, nodeName);
    }

    static void replaceEnvironmentBean(SlotStoreEnvironmentBean bean) throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(
                BeanPopulator.class,
                MethodHandles.lookup()
        );

        // Get a VarHandle for the private static field
        VarHandle varHandle = lookup.findStaticVarHandle(
                BeanPopulator.class,
                "beanInstances",
                ConcurrentMap.class
        );

        @SuppressWarnings("unchecked")
        ConcurrentMap<String, Object> beanInstances = (ConcurrentMap<String, Object>) varHandle.get();

        beanInstances.put(SlotStoreEnvironmentBean.class.getName(), bean);
    }
}