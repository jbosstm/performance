/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package com.arjuna.ats.jta.xa.performance;

import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.internal.arjuna.objectstore.ShadowNoFileLockStore;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
public class ShadowNoFileLockStoreBenchmark extends JTAStoreBase {
    @Setup(Level.Trial)
    @BeforeClass
    public static void setup() throws CoreEnvironmentBeanException {
        JTAStoreBase.setup(ShadowNoFileLockStore.class.getName());
    }

    @Test
    @Benchmark
    public void testShadowNoFileLockStore(Blackhole bh) {
        bh.consume(super.jtaTest());
    }
}