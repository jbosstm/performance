package org.jboss.narayana.performance.jts.service.first;

import javax.annotation.Resource;
import javax.ejb.RemoteHome;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

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
