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

import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.jta.xa.performance.JMHConfigJTA;
import com.atomikos.icatch.config.UserTransactionServiceImp;
import com.atomikos.icatch.jta.UserTransactionManager;

import org.junit.Before;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.CommandLineOptionException;

import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAResource;

@State(Scope.Benchmark)
public class AtomikosComparison extends ProductComparison {
    public static void main(String[] args) throws RunnerException, CommandLineOptionException, CoreEnvironmentBeanException {
        JMHConfigJTA.runJTABenchmark(AtomikosComparison.class.getSimpleName(), args);
    }

    protected ProductInterface getProductInterface() {
        return atomikos;
    }

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
}
