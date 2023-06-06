/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package com.arjuna.ats.jta.xa.performance;

import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.internal.arjuna.objectstore.VolatileStore;
import org.junit.BeforeClass;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.CommandLineOptionException;

/*
 config priority order is:
 1) Runner options
 2) method level annotations
 3) class level annotations
 */
//@Warmup(iterations = JMHConfigJTA.WI, time = JMHConfigJTA.WT)//, timeUnit = JMHConfigJTA.WTU)
//@Measurement(iterations = JMHConfigJTA.MI, time = JMHConfigJTA.MT)//, timeUnit = JMHConfigJTA.MTU)
//@Fork(JMHConfigJTA.BF)
//@Threads(JMHConfigJTA.BT)
@State(Scope.Benchmark)
public class JTAStoreTests extends JTAStoreBase {
    @Setup(Level.Trial)
    @BeforeClass
    public static void setup() throws CoreEnvironmentBeanException {
        JTAStoreBase.setup(VolatileStore.class.getName());
    }

    @Benchmark
    public void jtaTest(Blackhole bh) {
        bh.consume(super.jtaTest());
    }

    public static void main(String[] args) throws RunnerException, CommandLineOptionException, CoreEnvironmentBeanException {
        JMHConfigJTA.runJTABenchmark(JTAStoreTests.class.getSimpleName(), args);
    }
}