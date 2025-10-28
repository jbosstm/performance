/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */
package org.jboss.narayana.stm;

import com.arjuna.ats.arjuna.common.CoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;

public class LocalJTABenchmark {
    @State(Scope.Benchmark)
    static
    public class LocalJTABenchmarkState {
        XAResource xaResource;
        jakarta.transaction.TransactionManager tm;

        @Setup(Level.Invocation)
        public void doSetup() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, CoreEnvironmentBeanException {
            tm = com.arjuna.ats.jta.TransactionManager.transactionManager();

            if (tm == null) {
                throw new RuntimeException("Error - could not get transaction manager!");
            }

            xaResource = new XAResource() {
                @Override
                public void commit(Xid xid, boolean onePhase) throws XAException {
                }

                @Override
                public void end(Xid xid, int flags) throws XAException {
                }

                @Override
                public void forget(Xid xid) throws XAException {
                }

                @Override
                public int getTransactionTimeout() throws XAException {
                    return 0;
                }

                @Override
                public boolean isSameRM(XAResource xares) throws XAException {
                    return false;
                }

                @Override
                public int prepare(Xid xid) throws XAException {
                    return 0;
                }

                @Override
                public Xid[] recover(int flag) throws XAException {
                    return new Xid[0];
                }

                @Override
                public void rollback(Xid xid) throws XAException {
                }

                @Override
                public boolean setTransactionTimeout(int seconds) throws XAException {
                    return false;
                }

                @Override
                public void start(Xid xid, int flags) throws XAException {
                }
            };

            BeanPopulator.getDefaultInstance(CoreEnvironmentBean.class).setNodeIdentifier("1");
        }
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Fork(2)
    @Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
    public void benchmark(LocalJTABenchmarkState state) throws Exception {
        state.tm.begin();

        jakarta.transaction.Transaction theTransaction = state.tm.getTransaction();

        if (theTransaction != null) {
            if (!theTransaction.enlistResource(state.xaResource)) {
                System.err.println("Error - could not enlist resource in transaction!");

                state.tm.rollback();

                System.exit(1);
            }

            state.tm.commit();
        } else {
            System.err.println("Error - could not get transaction!");

            try {
                state.tm.rollback();
            } catch (Exception e) {
                System.exit(1);
            }
        }
    }
}
