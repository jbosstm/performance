package org.jboss.narayana.performance.jts.service.first;

import jakarta.annotation.Resource;
import jakarta.ejb.RemoteHome;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

import org.jboss.logging.Logger;
import org.jboss.narayana.performance.jts.service.AbstractEJB;

@RemoteHome(FirstEJBHome.class)
@Stateless
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class FirstEJBImpl extends AbstractEJB {

    private static final Logger LOG = Logger.getLogger(FirstEJBImpl.class);

    @Resource(lookup = "java:jboss/TransactionManager")
    private TransactionManager transactionManager;

    public void execute() {
        if (LOG.isTraceEnabled()) {
            LOG.trace("FirstEJBImpl.execute()");
        }

        try {
            final Transaction transaction = transactionManager.getTransaction();

            enlistDummyXAResource(transaction, "FirstEJBImpl_Dummy1");
            enlistDummyXAResource(transaction, "FirstEJBImpl_Dummy2");
        } catch (Exception e) {
            LOG.warn("Failed to enlist DummyXAResource in first EJB.", e);
        }
    }

}
