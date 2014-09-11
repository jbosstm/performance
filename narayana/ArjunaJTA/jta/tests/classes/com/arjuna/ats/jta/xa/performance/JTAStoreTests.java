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
import org.openjdk.jmh.annotations.Benchmark;

import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Level;


import javax.transaction.TransactionManager;

@State(Scope.Benchmark)
public class JTAStoreTests {

    @State(Scope.Benchmark)
	public static class BenchmarkState {
        TransactionManager tm;

		public BenchmarkState() {
          tm = com.arjuna.ats.jta.TransactionManager.transactionManager();
          try {
              BeanPopulator.getDefaultInstance(CoreEnvironmentBean.class).setNodeIdentifier("0");
          } catch (CoreEnvironmentBeanException e) {
              throw new RuntimeException(e);
          }

          BeanPopulator.getDefaultInstance(ObjectStoreEnvironmentBean.class).
              setObjectStoreType(VolatileStore.class.getName());
		}
    }

    @Setup(Level.Trial)
    public void before() {
    }

    @TearDown(Level.Trial)
    public void after() {
    }

    @Benchmark
    public void jtaTest(BenchmarkState state) {
        try {
            state.tm.begin();

            state.tm.getTransaction().enlistResource(new XAResourceImpl());
            state.tm.getTransaction().enlistResource(new XAResourceImpl());

            state.tm.commit();
        } catch(Exception e) {
            System.err.printf("JTAStoreTests#jtaTest: %s%n", e.getMessage());
        }
    }
}
