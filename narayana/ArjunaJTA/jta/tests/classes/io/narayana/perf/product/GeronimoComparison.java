/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package io.narayana.perf.product;

import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.jta.xa.performance.JMHConfigJTA;
import org.apache.geronimo.transaction.GeronimoUserTransaction;
import org.apache.geronimo.transaction.log.HOWLLog;
import org.apache.geronimo.transaction.manager.TransactionManagerImpl;
import org.apache.geronimo.transaction.manager.XidFactory;
import org.apache.geronimo.transaction.manager.XidFactoryImpl;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.CommandLineOptionException;

import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAResource;
import java.io.File;

@State(Scope.Benchmark)
public class GeronimoComparison extends ProductComparison {
    public static void main(String[] args) throws RunnerException, CommandLineOptionException, CoreEnvironmentBeanException {
        JMHConfigJTA.runJTABenchmark(GeronimoComparison.class.getSimpleName(), args);
    }

    protected ProductInterface getProductInterface() {
        return geronimo;
    }

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
        public XAResource getXAResource() {
            return new GeronimoXAResource();
        }

        @Override
        public void init() {
            try {
                tm = createTransactionManager();
                ut = new GeronimoUserTransaction(tm);
            } catch (Exception e) {
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
                    400, //                "maxBlocksPerFile",
                    16, //                "maxBuffers",
                    100, //                "maxLogFiles",
                    16, //                "minBuffers"
                    10,//                "threadsWaitingForceThreshold"});
                    xidFactory,
                    new File(buildDir)
            );

            howlLog.doStart();

            // NB to test without recovery use new GeronimoTransactionManager()
            return new TransactionManagerImpl(TX_TIMEOUT, xidFactory, howlLog);
        }
    };
}