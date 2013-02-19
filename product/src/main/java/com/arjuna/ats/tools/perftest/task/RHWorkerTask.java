package com.arjuna.ats.tools.perftest.task;



import com.arjuna.ats.internal.jts.orbspecific.recovery.RecoveryEnablement;
import com.arjuna.ats.tools.perftest.common.ORBWrapper;

import javax.transaction.TransactionManager;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

public class RHWorkerTask extends WorkerTask {
    private ORBWrapper orbWrapper;
    protected boolean jts = false;
    protected boolean stats = false;
    protected String objectStoreDir = "target/TxStoreDir";
    protected String objectStoreType = "com.arjuna.ats.internal.arjuna.objectstore.ShadowNoFileLockStore";
    private String errorReport = null;

    protected RHWorkerTask(CyclicBarrier cyclicBarrier, AtomicInteger count, int batch_size) {
        super(cyclicBarrier, count, batch_size);
    }

    @Override
    protected void init(Properties config) {
        jts = (config.getProperty("jts", "false")).equals("true");
        stats = (config.getProperty("stats", "false")).equals("true");
        objectStoreDir = config.getProperty("objectStoreDir", objectStoreDir);
        objectStoreType = config.getProperty("objectStoreType", objectStoreType);

        System.out.printf("Testing %s in %s mode\n\tobject store type: %s\n\tobject store directory: %s\n",
                getName(), (jts ? "JTS" : "JTA"), objectStoreType, objectStoreDir);
    }

    protected void postInit() {
        if (jts) {
            orbWrapper = new ORBWrapper();
            orbWrapper.start();
        }
    }

    @Override
    protected void fini() {
        super.fini();

        if (orbWrapper != null)
            orbWrapper.stop();
    }

    @Override
    protected TransactionManager getTransactionManager() {
        return com.arjuna.ats.jta.TransactionManager.transactionManager();
    }

    @Override
    protected String getName() {
        return "RH TM";
    }

    protected boolean validateRun(long committed, long aborted) {
        System.out.printf("\ncommitted: %d aborted: %d\n", committed,aborted);

        int txCount = Integer.valueOf(System.getProperty("iterations", "-1"));

        if (aborted != 0 && txCount != -1) {
            errorReport = String.format("%d out of %d transactions aborted\n", aborted, txCount);
            System.err.printf("%s\n", errorReport);

            return false;
        }

        return true;
    }

    public void reportErrors(PrintWriter output) {
        if (errorReport != null)
            output.printf("%s\n", errorReport);
    }
}
