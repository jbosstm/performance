/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package com.arjuna.ats.jta.xa.performance;

import com.arjuna.ats.internal.arjuna.objectstore.slot.jgroups.JGroupsRaftSlots;
import com.arjuna.ats.internal.arjuna.objectstore.slot.jgroups.JGroupsRaftStoreEnvironmentBean;
import org.jgroups.JChannel;
import org.jgroups.protocols.raft.RAFT;
import org.jgroups.protocols.raft.Role;
import org.jgroups.raft.blocks.ReplicatedStateMachine;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Multi-node Raft cluster benchmark measuring the impact of {@code allowDirtyReads}.
 * <p>
 * Creates a 3-node Raft cluster in-process (via SHARED_LOOPBACK), writes pre-populated
 * data through the leader, then benchmarks reads from a follower node. With dirty reads
 * enabled, reads hit the follower's local in-memory map; with dirty reads disabled,
 * reads go through the Raft leader via consensus.
 * <p>
 * The benchmark measures raw slot I/O (slots.read() and slots.write()) with no transaction overhead.
 * The *StoreBenchmark classes measure end-to-end transaction throughput through the full JTA stack.
 * Consequently one should expect significant performance difference between the benchmarks.
 * <p>
 * Run: {@code java -jar benchmarks.jar "JGroupsRaftClusterStoreBenchmark" -t 10 -f 1 -i 5 -wi 2 -r 3}
 */
@State(Scope.Benchmark)
public class JGroupsRaftClusterStoreBenchmark {

    private static final String STORE_BASE_DIR = System.getProperty("user.dir") + "/target/jgroups-raft-cluster-benchmark";
    private static final String[] NODE_NAMES = {"node1", "node2", "node3"};
    private static final String RAFT_MEMBERS = String.join(",", NODE_NAMES);
    private static final int NUM_SLOTS = 256;
    private static final int PREPOPULATE_SLOTS = 64;
    private static final byte[] SLOT_DATA = "benchmark-data-payload".getBytes();

    @Param({"true", "false"})
    private boolean allowDirtyReads;

    private JChannel[] channels;
    private JGroupsRaftSlots[] allSlots;
    private JGroupsRaftSlots leaderSlots;
    private JGroupsRaftSlots followerSlots;

    private final AtomicInteger readCounter = new AtomicInteger(0);
    private final AtomicInteger writeCounter = new AtomicInteger(0);

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(JGroupsRaftClusterStoreBenchmark.class.getSimpleName())
                .timeUnit(TimeUnit.SECONDS)
                .threads(10)
                .forks(1)
                .mode(Mode.Throughput)
                .warmupIterations(2)
                .warmupTime(TimeValue.seconds(2))
                .measurementIterations(5)
                .measurementTime(TimeValue.seconds(3))
                .shouldDoGC(true)
                .build();

        new Runner(opt).run();
    }

    @Setup(Level.Trial)
    @SuppressWarnings("unchecked")
    public void setup() throws Exception {
        purgeDirectory(new File(STORE_BASE_DIR));

        String clusterName = "raft-cluster-bm-" + System.currentTimeMillis();
        channels = new JChannel[3];
        allSlots = new JGroupsRaftSlots[3];
        ReplicatedStateMachine<Integer, byte[]>[] stateMachines = new ReplicatedStateMachine[3];
        JGroupsRaftStoreEnvironmentBean[] configs = new JGroupsRaftStoreEnvironmentBean[3];

        // Phase 1: Create and connect all channels before init — Raft needs a quorum
        // for leader election, so all nodes must be connected before any can elect.
        for (int i = 0; i < 3; i++) {
            String storeDir = STORE_BASE_DIR + "/" + NODE_NAMES[i];
            channels[i] = new JChannel("jgroups-raft.xml").name(NODE_NAMES[i]);

            RAFT raft = channels[i].getProtocolStack().findProtocol(RAFT.class);
            raft.logDir(storeDir);
            raft.logUseFsync(false);
            raft.members(Arrays.asList(RAFT_MEMBERS.split(",")));

            stateMachines[i] = new ReplicatedStateMachine<>(channels[i]);
            stateMachines[i].raftId(NODE_NAMES[i]);
            stateMachines[i].allowDirtyReads(allowDirtyReads);
            stateMachines[i].timeout(5000);

            channels[i].connect(clusterName);
        }

        // Wait for leader election across the cluster
        waitForLeader(channels, 10_000);

        // Phase 2: Create JGroupsRaftSlots with pre-configured channels
        for (int i = 0; i < 3; i++) {
            configs[i] = new JGroupsRaftStoreEnvironmentBean();
            configs[i].setExperimentalEnabled(true);
            configs[i].setJGroupsConfigFileName("jgroups-raft.xml");
            configs[i].setNodeAddress(NODE_NAMES[i]);
            configs[i].setClusterName(clusterName);
            configs[i].setCacheName(clusterName);
            configs[i].setStoreDir(STORE_BASE_DIR + "/" + NODE_NAMES[i]);
            configs[i].setNumberOfSlots(NUM_SLOTS);
            configs[i].setRaftMembers(RAFT_MEMBERS);
            configs[i].setRaftLogFsync(false);
            configs[i].setRaftTimeout(5000);
            configs[i].setAllowDirtyReads(allowDirtyReads);
            configs[i].setPreConfiguredChannel(channels[i]);
            configs[i].setPreConfiguredStateMachine(stateMachines[i]);

            allSlots[i] = new JGroupsRaftSlots();
            allSlots[i].init(configs[i]);
        }

        // Identify leader and a follower
        for (JGroupsRaftSlots slots : allSlots) {
            String role = slots.getRole();
            if (Role.Leader.name().equals(role) && leaderSlots == null) {
                leaderSlots = slots;
            } else if (Role.Follower.name().equals(role) && followerSlots == null) {
                followerSlots = slots;
            }
        }

        if (leaderSlots == null || followerSlots == null) {
            throw new IllegalStateException("Cluster did not form correctly: leader=" + leaderSlots + " follower=" + followerSlots);
        }

        // Pre-populate slots via the leader
        for (int i = 0; i < PREPOPULATE_SLOTS; i++) {
            leaderSlots.write(i, SLOT_DATA, true);
        }

        // Wait for the follower to apply all replicated entries
        RAFT leaderRaft = leaderSlots.getChannel().getProtocolStack().findProtocol(RAFT.class);
        RAFT followerRaft = followerSlots.getChannel().getProtocolStack().findProtocol(RAFT.class);
        long targetIndex = leaderRaft.commitIndex();
        long deadline = System.currentTimeMillis() + 10_000;

        while (followerRaft.lastAppended() < targetIndex) {
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException(
                        "Follower replication timed out: lastApplied=" + followerRaft.lastAppended()
                                + " target=" + targetIndex);
            }
            TimeUnit.MILLISECONDS.sleep(50);
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (channels != null) {
            for (JChannel ch : channels) {
                if (ch != null) {
                    try {
                        ch.close();
                    } catch (Exception ignore) {
                    }
                }
            }
        }

        purgeDirectory(new File(STORE_BASE_DIR));
    }

    @Benchmark
    public void readFromFollower(Blackhole bh) throws IOException {
        int slotId = readCounter.getAndIncrement() % PREPOPULATE_SLOTS;
        bh.consume(followerSlots.read(slotId));
    }

    @Benchmark
    public void writeToLeader(Blackhole bh) throws IOException {
        int slotId = writeCounter.getAndIncrement() % NUM_SLOTS;
        leaderSlots.write(slotId, SLOT_DATA, true);
    }

    private static void waitForLeader(JChannel[] channels, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            for (JChannel ch : channels) {
                RAFT raft = ch.getProtocolStack().findProtocol(RAFT.class);
                if (raft != null && raft.leader() != null) {
                    return;
                }
            }
            TimeUnit.MILLISECONDS.sleep(50);
        }
        throw new IllegalStateException("No Raft leader elected within " + timeoutMs + "ms");
    }

    private static void purgeDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    purgeDirectory(file);
                }
            }
            dir.delete();
        }
    }
}
