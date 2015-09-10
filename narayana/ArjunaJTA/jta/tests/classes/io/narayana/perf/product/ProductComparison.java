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

import com.arjuna.ats.jta.xa.performance.XAResourceImpl;
import com.atomikos.icatch.config.UserTransactionServiceImp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.runner.BenchmarkException;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.CommandLineOptionException;
import com.arjuna.ats.jta.xa.performance.JMHConfigJTA;

import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAResource;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.utils.DefaultExceptionAnalyzer;

import com.arjuna.ats.arjuna.common.CoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.internal.arjuna.objectstore.hornetq.HornetqJournalEnvironmentBean;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;

import com.atomikos.icatch.jta.UserTransactionManager;

import org.apache.geronimo.transaction.log.HOWLLog;
import org.apache.geronimo.transaction.manager.TransactionManagerImpl;
import org.apache.geronimo.transaction.manager.XidFactory;
import org.apache.geronimo.transaction.manager.XidFactoryImpl;
import org.apache.geronimo.transaction.GeronimoUserTransaction;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@State(Scope.Benchmark)
public class ProductComparison {
    final private static String outerClassName = ProductComparison.class.getName();

    private Map<String, ProductWorker> workers;

    public static void main(String[] args) throws RunnerException, CommandLineOptionException, CoreEnvironmentBeanException {
        JMHConfigJTA.runJTABenchmark(ProductComparison.class.getSimpleName(), args);
    }

    @Test
    @Benchmark
    public void testNarayana() throws Exception {
        doWork(narayana);
    }

    @Test
    @Benchmark
    public void testJotm() throws Exception {
        doWork(jotm);
    }

    @Test
    @Benchmark
    public void testBitronix() throws Exception {
       doWork(bitronix);
    }

    @Test
    @Benchmark
    public void testAtomikos() throws Exception {
        doWork(atomikos);
    }

    @Test
    @Benchmark
    public void testGeronimo() throws Exception {
        doWork(geronimo);
    }

    @Before
    @Setup
    public void setup() {
        workers = new HashMap<>();

        addWorker(new ProductWorker<Void>(atomikos));
        addWorker(new ProductWorker<Void>(jotm));
        addWorker(new ProductWorker<Void>(narayana));
        addWorker(new ProductWorker<Void>(geronimo));
        addWorker(new BitronixWorker<Void>(bitronix));
    }

    @After
    @TearDown
    public void tearDown() {
        for (ProductWorker product : workers.values()) {
            product.fini();
        }
    }

    // define workers for each product to be tested

    private void addWorker(ProductWorker worker) {
        assertFalse("Duplicate product name", workers.containsKey(worker.getName()));

        workers.put(worker.getName(), worker);

        worker.init();
    }

    private void doWork(ProductInterface product) throws Exception {
        assertTrue(workers.containsKey(product.getName()));

        try {
            workers.get(product.getName()).doWork();
        } catch (Exception e) {
//            System.err.printf("%s: %s%n", product.getName(), e.getMessage());
            throw new BenchmarkException(e);
        }
    }

    private ProductInterface narayana = new ProductInterface() {
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
            return outerClassName + "_Narayana";
        }

        @Override
        public XAResource getXAResource() {
            return new XAResourceImpl();
        }

        @Override
        public void init() {
            String storeDir = BeanPopulator.getDefaultInstance(ObjectStoreEnvironmentBean.class).getObjectStoreDir();

            BeanPopulator.getDefaultInstance(HornetqJournalEnvironmentBean.class).setStoreDir(storeDir);
            BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "communicationStore").setObjectStoreDir(storeDir);

            try {
                BeanPopulator.getDefaultInstance(CoreEnvironmentBean.class).setNodeIdentifier("0");
            } catch (CoreEnvironmentBeanException e) {
                e.printStackTrace();
            }
            HornetqJournalEnvironmentBean hornetqJournalEnvironmentBean = BeanPopulator.getDefaultInstance(
                com.arjuna.ats.internal.arjuna.objectstore.hornetq.HornetqJournalEnvironmentBean.class
                );
            hornetqJournalEnvironmentBean.setAsyncIO(true);
            hornetqJournalEnvironmentBean.setStoreDir(storeDir);//"HornetqObjectStore");
            BeanPopulator.getDefaultInstance(ObjectStoreEnvironmentBean.class).setObjectStoreType("com.arjuna.ats.internal.arjuna.objectstore.hornetq.HornetqObjectStoreAdaptor");
            ut = com.arjuna.ats.jta.UserTransaction.userTransaction();
            tm = com.arjuna.ats.jta.TransactionManager.transactionManager();
        }

        @Override
        public void fini() {
        }
    };

    private ProductInterface jotm = new ProductInterface() {
        org.objectweb.jotm.Jotm tm;

        @Override
        public UserTransaction getUserTransaction() throws SystemException {
            return tm.getUserTransaction();
        }

        @Override
        public TransactionManager getTransactionManager() {
            return tm.getTransactionManager();
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
        public XAResource getXAResource() {
            return new XAResourceImpl();
        }

        @Override
        public void init() {
            try {
                tm = new org.objectweb.jotm.Jotm(true, false);
            } catch (javax.naming.NamingException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void fini() {
            if (tm != null) {
                org.objectweb.jotm.TimerManager.stop();
                tm.stop();
                tm = null;
            }
        }
    };

    private ProductInterface bitronix = new ProductInterface() {
        private BitronixTransactionManager btm;

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
        public XAResource getXAResource() {
            return new XAResourceImpl();
        }

        @Override
        public void init() {
            TransactionManagerServices.getConfiguration().setGracefulShutdownInterval(1);
            TransactionManagerServices.getConfiguration().setExceptionAnalyzer(DefaultExceptionAnalyzer.class.getName());
            btm = TransactionManagerServices.getTransactionManager();
        }

        @Override
        public void fini() {
            if (btm != null) {
                btm.shutdown();
                btm = null;
            }
        }
    };

    private ProductInterface atomikos = new ProductInterface() {
        private UserTransactionManager utm;

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
        public XAResource getXAResource() {
            return new AomikosXAResource();
        }

        @Override
        public void init() {
            System.setProperty(UserTransactionServiceImp.HIDE_INIT_FILE_PATH_PROPERTY_NAME, "no thanks");

            utm = new UserTransactionManager();

            try {
                utm.init();
            } catch (SystemException e) {
                e.printStackTrace();
                fini();
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

    private ProductInterface geronimo = new ProductInterface() {
        private final String LOG_FILE_NAME = "geronimo_howl_test_";
        private HOWLLog howlLog;
        private UserTransaction ut;
        private TransactionManager tm;

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
            return "geronimo";
        }

        @Override
        public String getNameOfMetric() {
            return outerClassName + "_Geronimo";
        }

        @Override
        public XAResource getXAResource() {
            return new GeronimoXAResource();
        }

        @Override
        public void init() {
            try {
                tm = createTransactionManager();
                ut = new GeronimoUserTransaction(tm);
            } catch (Exception e ) {
                try {
                    fini();
                } catch (RuntimeException re) {
                }
                throw new RuntimeException(e);
            }
        }

        @Override
        public void fini() {
            if (howlLog != null) {
                try {
                    howlLog.doStop();
                    howlLog = null;
                } catch (Exception e) {
                    // don't know how to handle it
                    throw new RuntimeException(e);
                }
            }
        }

        protected TransactionManagerImpl createTransactionManager() throws Exception {
            String buildDir = System.getProperty("BUILD_DIR", "target");
            XidFactory xidFactory = new XidFactoryImpl("hi".getBytes());

            buildDir = buildDir + "/geronimo";

            howlLog = new HOWLLog(
                    "org.objectweb.howl.log.BlockLogBuffer", //                "bufferClassName",
                    4, //                "bufferSizeKBytes",
                    true, //                "checksumEnabled",
                    true, //                "adler32Checksum",
                    20, //                "flushSleepTime",
                    buildDir, //                "logFileDir",
                    "log", //                "logFileExt",
                    LOG_FILE_NAME, //                "logFileName",
                    200, //                "maxBlocksPerFile",
                    10, //                "maxBuffers",                       log
                    2, //                "maxLogFiles",
                    2, //                "minBuffers"
                    10,//                "threadsWaitingForceThreshold"});
                    xidFactory,
                    new File(buildDir)
            );

            howlLog.doStart();

            // NB to test without recovery use new GeronimoTransactionManager()
            return new TransactionManagerImpl(1, xidFactory, howlLog);
        }
    };
}
