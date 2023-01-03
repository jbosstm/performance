package org.jboss.narayana.performance.rts.service;

import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;

import org.jboss.logging.Logger;
import org.jboss.narayana.performance.common.xa.DummyXAResource;

public abstract class AbstractService {

    private static final Logger LOG = Logger.getLogger(AbstractService.class);

    public abstract void post();

    protected void enlistDummyXAResource(final Transaction transaction, final String name)
            throws IllegalStateException, RollbackException, SystemException {

        if (LOG.isTraceEnabled()) {
            LOG.trace("Enlisting XA resource: " + name);
        }

        transaction.enlistResource(new DummyXAResource(name));
    }

}
