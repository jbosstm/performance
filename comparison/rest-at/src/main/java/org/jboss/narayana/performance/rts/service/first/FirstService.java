package org.jboss.narayana.performance.rts.service.first;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.jboss.logging.Logger;
import org.jboss.narayana.performance.rts.service.AbstractService;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@Path("/")
@Stateless
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class FirstService extends AbstractService {

    private static final Logger LOG = Logger.getLogger(FirstService.class);

    @Resource(lookup = "java:jboss/TransactionManager")
    private TransactionManager transactionManager;

    @POST
    public void post() {
        if (LOG.isTraceEnabled()) {
            LOG.trace("FirstService.post()");
        }

        try {
            final Transaction transaction = transactionManager.getTransaction();

            enlistDummyXAResource(transaction, "FirstService_Dummy1");
            enlistDummyXAResource(transaction, "FirstService_Dummy2");
        } catch (Exception e) {
            LOG.warn("Failed to enlist DummyXAResource in first service.", e);
        }
    }

}
