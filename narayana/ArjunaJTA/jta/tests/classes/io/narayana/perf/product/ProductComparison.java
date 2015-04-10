/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package io.narayana.perf.product;

import javax.sql.DataSource;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.CommandLineOptionException;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.utils.DefaultExceptionAnalyzer;

import com.arjuna.ats.arjuna.common.CoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.internal.arjuna.objectstore.hornetq.HornetqJournalEnvironmentBean;
import com.arjuna.ats.jta.xa.performance.JMHConfigJTA;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;
import com.atomikos.icatch.jta.UserTransactionManager;

@State(Scope.Benchmark)
public class ProductComparison {
    final protected static String METHOD_SEP = "_";
    final private static String outerClassName = ProductComparison.class.getName();
    final static private String narayanaMetricName = outerClassName + METHOD_SEP + "Narayana";

    public static void main(String[] args) throws RunnerException, CommandLineOptionException, CoreEnvironmentBeanException {
        JMHConfigJTA.runJTABenchmark(ProductComparison.class.getSimpleName(), args);
    }

    protected void runTest(ProductWorker worker) throws Exception {
        worker.doWork();
    }

    @Test
    @Benchmark
    public void testNarayana() throws Exception {
        narayanaWorker.doWork();
    }

    @Test
    @Benchmark
    public void testJotm() throws Exception {
        jotmWorker.doWork();
    }

    @Test
    @Benchmark
    public void testBitronix() throws Exception {
        bitronixWorker.doWork();
    }

    @Test
    @Benchmark
    public void testAtomikos() throws Exception {
        atomikosWorker.doWork();
    }

    ProductInterface narayana = new ProductInterface() {
        UserTransaction ut;
        TransactionManager tm;

        @Override
        public UserTransaction getUserTransaction() throws SystemException {
            return ut;
        }

        @Override
        public TransactionManager getTransactionManager() {
            return tm;
        }

        @Override
        public String getName() {
            return "narayana";
        }

        @Override
        public String getNameOfMetric() {
            return narayanaMetricName;
        }

        @Override
        public void init() {
            try {
                BeanPopulator.getDefaultInstance(CoreEnvironmentBean.class).setNodeIdentifier("0");
            } catch (CoreEnvironmentBeanException e) {
                e.printStackTrace();
            }
            HornetqJournalEnvironmentBean hornetqJournalEnvironmentBean = BeanPopulator.getDefaultInstance(
                com.arjuna.ats.internal.arjuna.objectstore.hornetq.HornetqJournalEnvironmentBean.class
                );
            hornetqJournalEnvironmentBean.setAsyncIO(true);
            hornetqJournalEnvironmentBean.setStoreDir("HornetqObjectStore");
            BeanPopulator.getDefaultInstance(ObjectStoreEnvironmentBean.class).setObjectStoreType("com.arjuna.ats.internal.arjuna.objectstore.hornetq.HornetqObjectStoreAdaptor");
            ut = com.arjuna.ats.jta.UserTransaction.userTransaction();
            tm = com.arjuna.ats.jta.TransactionManager.transactionManager();
        }

        @Override
        public void fini() {
        }
    };

    ProductInterface jotm = new ProductInterface() {
        org.objectweb.jotm.Jotm jotm;

        @Override
        public UserTransaction getUserTransaction() throws SystemException {
            return jotm.getUserTransaction();
        }

        @Override
        public TransactionManager getTransactionManager() {
            return jotm.getTransactionManager();
        }

        @Override
        public String getName() {
            return "jotm";
        }

        @Override
        public String getNameOfMetric() {
            return outerClassName + "_Jotm";
        }

        @Override
        public void init() {
            try {
                jotm = new org.objectweb.jotm.Jotm(true, false);
            } catch (javax.naming.NamingException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void fini() {
            org.objectweb.jotm.TimerManager.stop();
            jotm.stop();
        }
    };

    ProductInterface bitronix = new ProductInterface() {
        private BitronixTransactionManager btm;
        private DataSource ds;

        @Override
        public UserTransaction getUserTransaction() {
            return btm;
        }

        @Override
        public TransactionManager getTransactionManager() {
            return btm;
        }

        @Override
        public String getName() {
            return "bitronix";
        }

        @Override
        public String getNameOfMetric() {
            return outerClassName + "_Bitronix";
        }

        @Override
        public void init() {
            TransactionManagerServices.getConfiguration().setGracefulShutdownInterval(1);
            TransactionManagerServices.getConfiguration().setExceptionAnalyzer(DefaultExceptionAnalyzer.class.getName());
            btm = TransactionManagerServices.getTransactionManager();
        }

        @Override
        public void fini() {
            btm.shutdown();
        }
    };

    ProductInterface atomikos = new ProductInterface() {
        UserTransactionManager utm;

        @Override
        public UserTransaction getUserTransaction() throws SystemException {
            return new com.atomikos.icatch.jta.UserTransactionImp();
        }

        @Override
        public TransactionManager getTransactionManager() {
            return utm;
        }

        @Override
        public String getName() {
            return "atomikos";
        }

        @Override
        public String getNameOfMetric() {
            return outerClassName + "_Atomkos";
        }

        @Override
        public void init() {
            utm = new UserTransactionManager();

            try {
                utm.init();
            } catch (SystemException e) {
                e.printStackTrace();
                utm.close();
                utm = null;
            }
        }

        @Override
        public void fini() {
            if (utm != null) {
                utm.close();
                utm = null;
            }
        }
    };

    ProductWorker<Void> atomikosWorker;
    BitronixWorker<Void> bitronixWorker;
    ProductWorker<Void> jotmWorker;
    ProductWorker<Void> narayanaWorker;

    @Before
    @Setup
    public void setup() {
        atomikosWorker = new ProductWorker<Void>(atomikos);
        bitronixWorker = new BitronixWorker<Void>(bitronix);
        jotmWorker = new ProductWorker<Void>(jotm);
        narayanaWorker = new ProductWorker<Void>(narayana);

        atomikosWorker.init();
        bitronixWorker.init();
        jotmWorker.init();
        narayanaWorker.init();
    }

    @After
    @TearDown
    public void tearDown() {
        narayanaWorker.fini();
        jotmWorker.fini();
        bitronixWorker.fini();
        atomikosWorker.fini();
    }
}
