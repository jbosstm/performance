package org.jboss.narayana.performance.xts.client;

import io.narayana.perf.Measurement;
import io.narayana.perf.Worker;

import java.net.MalformedURLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.TransactionManager;

import org.jboss.logging.Logger;
import org.jboss.narayana.performance.xts.service.first.FirstService;
import org.jboss.narayana.performance.xts.service.first.FirstServiceClientFactory;
import org.jboss.narayana.performance.xts.service.second.SecondService;
import org.jboss.narayana.performance.xts.service.second.SecondServiceClientFactory;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class TestWorkerImpl implements Worker<String> {

    private static final Logger LOG = Logger.getLogger(TestWorkerImpl.class);

    private FirstService firstService = null;

    private SecondService secondService = null;

    private TransactionManager transactionManager = null;

    @Override
    public String doWork(final String context, final int batchSize, final Measurement<String> measurement) {
        for (int i = 0; i < batchSize; i++) {
            executeIteration();
        }

        return null;
    }

    @Override
    public void finishWork(Measurement<String> measurement) {
    }

    @Override
    public void init() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Starting initialization of the worker.");
        }

        try {
            firstService = FirstServiceClientFactory.getInstance();
        } catch (MalformedURLException e) {
            LOG.warn("Failed to initialize first test service client.", e);
            throw new RuntimeException("Failed to initialize first test service client.", e);
        }

        try {
            secondService = SecondServiceClientFactory.getInstance();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to initialize second test service client.", e);
        }

        try {
            transactionManager = (TransactionManager) new InitialContext().lookup("java:/jboss/TransactionManager");
        } catch (NamingException e) {
            LOG.warn("Failed to initialize transaction manager.", e);
            throw new RuntimeException("Failed to initialize transaction manager.", e);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Completed initialization of the worker.");
        }
    }

    @Override
    public void fini() {
        // pass for now
    }

    private void executeIteration() {
        try {
            transactionManager.begin();
            firstService.execute();
            secondService.execute();
            transactionManager.commit();
        } catch (final Throwable t) {
            LOG.warn("Failure during one of the executions.", t);

            try {
                transactionManager.rollback();
            } catch (final Throwable t2) {
                LOG.warn("Failed to rollback transaction", t2);
            }
        }
    }
}
