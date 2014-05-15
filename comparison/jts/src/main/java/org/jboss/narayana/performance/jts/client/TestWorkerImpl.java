package org.jboss.narayana.performance.jts.client;

import io.narayana.perf.Result;
import io.narayana.perf.Worker;

import java.rmi.RemoteException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.TransactionManager;

import org.jboss.logging.Logger;
import org.jboss.narayana.performance.jts.service.first.FirstEJB;
import org.jboss.narayana.performance.jts.service.first.FirstEJBHome;
import org.jboss.narayana.performance.jts.service.second.SecondEJB;
import org.jboss.narayana.performance.jts.service.second.SecondEJBHome;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class TestWorkerImpl implements Worker<String> {

    private static final Logger LOG = Logger.getLogger(TestWorkerImpl.class);

    private final FirstEJBHome firstEJBHome;

    private final SecondEJBHome secondEJBHome;

    private TransactionManager transactionManager;

    private FirstEJB firstEJB;

    private SecondEJB secondEJB;

    public TestWorkerImpl(final FirstEJBHome firstEJBHome, final SecondEJBHome secondEJBHome) {
        this.firstEJBHome = firstEJBHome;
        this.secondEJBHome = secondEJBHome;
    }

    public String doWork(String context, int iterationsCount, Result<String> options) {
        for (int i = 0; i < iterationsCount; i++) {
            executeIteration();
        }

        return null;
    }

    public void init() {
        try {
            firstEJB = firstEJBHome.create();
        } catch (final RemoteException e) {
            LOG.warn("Failed to create EJB");
            throw new RuntimeException("Failed to create first EJB");
        }

        try {
            secondEJB = secondEJBHome.create();
        } catch (final RemoteException e) {
            LOG.warn("Failed to create EJB");
            throw new RuntimeException("Failed to create second EJB");
        }

        try {
            transactionManager = (TransactionManager) new InitialContext().lookup("java:/jboss/TransactionManager");
        } catch (final NamingException e) {
            LOG.warn("Failed to initiate transaction manager", e);
            throw new RuntimeException("Failed to initiate transaction manager", e);
        }
    }

    public void fini() {
        try {
            firstEJB.remove();
        } catch (final Exception e) {
            LOG.warn("Failed to remote first EJB", e);
        }

        try {
            secondEJB.remove();
        } catch (final Exception e) {
            LOG.warn("Failed to remote second EJB", e);
        }
    }

    private void executeIteration() {
        try {
            transactionManager.begin();
            firstEJB.execute();
            secondEJB.execute();
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
