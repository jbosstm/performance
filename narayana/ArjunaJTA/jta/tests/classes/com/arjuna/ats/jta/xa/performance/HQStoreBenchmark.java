/*
 * Copyright 2015, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
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
