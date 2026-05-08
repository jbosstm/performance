/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package com.arjuna.ats.jta.xa.performance;

import com.arjuna.ats.arjuna.common.CoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import org.jboss.logging.Logger;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JTAStoreBase {
    static final String JMHARGS_ENV = "JMHARGS"; // for overriding the number of THREADS

    protected static final Logger log = Logger.getLogger(JTAStoreBase.class);
    private static XAResourceImpl resource1;
    private static XAResourceImpl resource2;
    private static TransactionManager tm;

    protected static int getThreadCountFromProperties(int defaultThreadCount) {
        int threadCount = defaultThreadCount;
        String jmhArgs = System.getenv(JMHARGS_ENV);

        if (jmhArgs != null) {
            /*
             * The number of slots should equal the maximum number of unresolved transactions expected at any given time,
             * including those in-flight and awaiting recovery so update the size based on the number of threads
             * used to run the benchmark.
             */
            // BeforeClass methods are not benchmarked so compile the pattern here
            String pat = "-t\\s+(\\d+)";
            Pattern pattern = Pattern.compile(pat);
            Matcher matcher = pattern.matcher(jmhArgs);
            if (matcher.find()) {
                try {
                    threadCount = Integer.parseInt(matcher.group(1));
                } catch (NumberFormatException e) {
                    System.err.printf("JMHARGS args -t option (%s) invalid%n", jmhArgs);
                }
            }

            System.out.printf("JMHARGS=%s%nUsing %d threads%n", jmhArgs, threadCount);
        }

        return threadCount;
    }

    protected static void cleanStore(File storeDir) {
        try {
            // clean up the storage otherwise it may interfere with the next run
            if (!purgeFiles(storeDir)) {
                System.err.printf("problem removing slot store file storage (%s)%n", storeDir);
            }
        } catch (Exception e) {
            System.err.printf("Warn: problem cleaning the object store (%s): %s%n", storeDir, e.getMessage());
        }
    }

    protected static boolean purgeFiles(File storeDir) {
        File[] files = storeDir.listFiles();

        if (files != null) {
            for (File file : files) {
                purgeFiles(file);
            }
        }

        return storeDir.delete();
    }

    protected static int roundUp(int increment, int value) {
        int res = value % increment;
        // round up to nearest multiple of increment (eg a h/w page size)
        if (res == 0) {
            return value;
        } else {
            return value + increment - res;
        }
    }

    protected static void setup(String storeType) throws CoreEnvironmentBeanException {
        BeanPopulator.getDefaultInstance(CoreEnvironmentBean.class).setNodeIdentifier("1");
        BeanPopulator.getDefaultInstance(ObjectStoreEnvironmentBean.class).setObjectStoreType(storeType);

        tm = com.arjuna.ats.jta.TransactionManager.transactionManager();
        resource1 = new XAResourceImpl();
        resource2 = new XAResourceImpl();
    }

    public boolean jtaTest() throws HeuristicRollbackException, SystemException, HeuristicMixedException, NotSupportedException, RollbackException {
        try {
            tm.begin();

            tm.getTransaction().enlistResource(resource1);
            tm.getTransaction().enlistResource(resource2);

            tm.commit();
        } catch(Exception e) {
            log.fatal("JTAStoreTests#jtaTest%n", e);
            throw new Error(e);
        }

        return true;
    }
}
