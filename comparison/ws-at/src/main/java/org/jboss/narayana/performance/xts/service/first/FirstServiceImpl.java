package org.jboss.narayana.performance.xts.service.first;

import jakarta.annotation.Resource;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

import org.jboss.logging.Logger;
import org.jboss.narayana.performance.common.xa.DummyXAResource;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@Stateless
@Remote(FirstService.class)
@SOAPBinding(style = SOAPBinding.Style.RPC)
@WebService(name = "FirstService", portName = "FirstService", serviceName = "FirstServiceService",
        targetNamespace = "http://www.narayana.io")
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class FirstServiceImpl implements FirstService {

    private static final Logger LOG = Logger.getLogger(FirstServiceImpl.class);

    @Resource(lookup = "java:jboss/TransactionManager")
    private TransactionManager transactionManager;

    public void execute() {
        try {
            Transaction transaction = transactionManager.getTransaction();
            transaction.enlistResource(new DummyXAResource("First dummy XA resource of first service"));
            transaction.enlistResource(new DummyXAResource("Second dummy XA resource of first service"));
        } catch (Exception e) {
            LOG.warn("Failed to enlist DummyXAResource in first service.", e);
        }
    }

}
