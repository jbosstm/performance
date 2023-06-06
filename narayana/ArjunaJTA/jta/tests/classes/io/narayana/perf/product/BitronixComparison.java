/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package io.narayana.perf.product;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.utils.DefaultExceptionAnalyzer;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.jta.xa.performance.JMHConfigJTA;
import com.arjuna.ats.jta.xa.performance.XAResourceImpl;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.CommandLineOptionException;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAResource;

@State(Scope.Benchmark)
public class BitronixComparison extends ProductComparison {
    public static void main(String[] args) throws RunnerException, CommandLineOptionException, CoreEnvironmentBeanException {
        JMHConfigJTA.runJTABenchmark(BitronixComparison.class.getSimpleName(), args);
    }

    protected ProductInterface getProductInterface() {
        return bitronix;
    }

    protected ProductWorker<Void> getProductWorker() {
        return new BitronixWorker<Void>(getProductInterface());
    }

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
}