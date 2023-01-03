package org.jboss.narayana.performance.xts.service.second;

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
@Remote(SecondService.class)
@SOAPBinding(style = SOAPBinding.Style.RPC)
@WebService(name = "SecondService", portName = "SecondService", serviceName = "SecondServiceService",
        targetNamespace = "http://www.narayana.io")
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class SecondServiceImpl implements SecondService {

    private static final Logger LOG = Logger.getLogger(SecondServiceImpl.class);

    @Resource(lookup = "java:jboss/TransactionManager")
    private TransactionManager transactionManager;

    public void execute() {
        try {
            Transaction transaction = transactionManager.getTransaction();
            transaction.enlistResource(new DummyXAResource("First dummy XA resource of second service"));
            transaction.enlistResource(new DummyXAResource("Second dummy XA resource of second service"));
        } catch (Exception e) {
            LOG.warn("Failed to enlist DummyXAResource in second service.", e);
        }
    }

}
