/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package io.narayana.perf.product;

import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.jta.xa.performance.JMHConfigJTA;
import com.arjuna.ats.jta.xa.performance.XAResourceImpl;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.CommandLineOptionException;

import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;
import javax.transaction.xa.XAResource;

@State(Scope.Benchmark)
public class NarayanaComparison extends ProductComparisonJakarta {
    public static void main(String[] args) throws RunnerException, CommandLineOptionException, CoreEnvironmentBeanException {
        JMHConfigJTA.runJTABenchmark(NarayanaComparison.class.getSimpleName(), args);
    }

    protected ProductInterfaceJakarta getProductInterface() {
        return narayana;
    }

    private ProductInterfaceJakarta narayana = new ProductInterfaceJakarta() {
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
        public XAResource getXAResource() {
            return new XAResourceImpl();
        }

        @Override
        public void init() {
            ut = com.arjuna.ats.jta.UserTransaction.userTransaction();
            tm = com.arjuna.ats.jta.TransactionManager.transactionManager();
        }

        @Override
        public void fini() {
        }
    };
}