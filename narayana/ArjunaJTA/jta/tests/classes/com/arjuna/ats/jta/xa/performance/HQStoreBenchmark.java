/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package com.arjuna.ats.jta.xa.performance;

import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.internal.arjuna.objectstore.hornetq.HornetqJournalEnvironmentBean;
import com.arjuna.ats.internal.arjuna.objectstore.hornetq.HornetqObjectStoreAdaptor;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
public class HQStoreBenchmark extends JTAStoreBase {

    @Setup(Level.Trial)
    @BeforeClass
    public static void setup() throws CoreEnvironmentBeanException {
        HornetqJournalEnvironmentBean hornetqJournalEnvironmentBean = BeanPopulator.getDefaultInstance(HornetqJournalEnvironmentBean.class);
        // Please keep the journal config in line with the journal config in narayana/ArjunaJTA/jta/etc/jbossts-properties.xml
        hornetqJournalEnvironmentBean.setAsyncIO(true);
        hornetqJournalEnvironmentBean.setSyncDeletes(false);
        hornetqJournalEnvironmentBean.setBufferFlushesPerSecond(300);
        hornetqJournalEnvironmentBean.setMaxIO(500);
        JTAStoreBase.setup(HornetqObjectStoreAdaptor.class.getName());
    }

    @Test
    @Benchmark
    public void testHQStore(Blackhole bh) {
        bh.consume(super.jtaTest());
    }
}