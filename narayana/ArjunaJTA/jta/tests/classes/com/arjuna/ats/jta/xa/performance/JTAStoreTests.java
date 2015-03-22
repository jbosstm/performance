/*
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
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

import com.arjuna.ats.arjuna.common.CoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.internal.arjuna.objectstore.VolatileStore;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;
import org.openjdk.jmh.annotations.*;

import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.*;

import javax.transaction.TransactionManager;

/*
 config priority order is:
 1) Runner options
 2) method level annotations
 3) class level annotations
 */
@Warmup(iterations = JMHConfigJTA.WI, time = JMHConfigJTA.WT)//, timeUnit = JMHConfigJTA.WTU)
@Measurement(iterations = JMHConfigJTA.MI, time = JMHConfigJTA.MT)//, timeUnit = JMHConfigJTA.MTU)
@Fork(JMHConfigJTA.BF)
@Threads(JMHConfigJTA.BT)
@State(Scope.Benchmark)
public class JTAStoreTests {
    static {
      try {
        BeanPopulator.getDefaultInstance(CoreEnvironmentBean.class).setNodeIdentifier("0");
        BeanPopulator.getDefaultInstance(ObjectStoreEnvironmentBean.class).setObjectStoreType(VolatileStore.class.getName());
        tm = com.arjuna.ats.jta.TransactionManager.transactionManager();
      } catch (Exception e) {
          throw new RuntimeException(e);
      }
    }

    private static TransactionManager tm;
    private XAResourceImpl resource1 = new XAResourceImpl();
    private XAResourceImpl resource2 = new XAResourceImpl();

    @Benchmark
    public boolean jtaTest() {
        try {
            tm.begin();

            tm.getTransaction().enlistResource(resource1);
            tm.getTransaction().enlistResource(resource2);

            tm.commit();
        } catch(Exception e) {
            System.err.printf("JTAStoreTests#jtaTest: %s%n", e.getMessage());
        }

        return true;
    }

    public static void main(String[] args) throws RunnerException, CommandLineOptionException, CoreEnvironmentBeanException {
        JMHConfigJTA.runJTABenchmark(JTAStoreTests.class.getSimpleName(), args);
    }
}
