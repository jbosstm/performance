package org.jboss.narayana.performance.jts.service.second;

import jakarta.annotation.Resource;
import jakarta.ejb.RemoteHome;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

import org.jboss.logging.Logger;
import org.jboss.narayana.performance.jts.service.AbstractEJB;

@RemoteHome(SecondEJBHome.class)
@Stateless
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class SecondEJBImpl extends AbstractEJB {

    private static final Logger LOG = Logger.getLogger(SecondEJBImpl.class);

    @Resource(lookup = "java:jboss/TransactionManager")
    private TransactionManager transactionManager;

    public void execute() {
        if (LOG.isTraceEnabled()) {
            LOG.trace("SecondEJBImpl.execute()");
        }

        try {
            final Transaction transaction = transactionManager.getTransaction();

            enlistDummyXAResource(transaction, "SecondEJBImpl_Dummy1");
            enlistDummyXAResource(transaction, "SecondEJBImpl_Dummy2");
        } catch (Exception e) {
            LOG.warn("Failed to enlist DummyXAResource in second EJB.", e);
        }
    }

}
