package org.jboss.narayana.performance.xts.service.first;

import javax.annotation.Resource;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

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
