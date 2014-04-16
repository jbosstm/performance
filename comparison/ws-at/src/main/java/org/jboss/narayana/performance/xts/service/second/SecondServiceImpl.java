package org.jboss.narayana.performance.xts.service.second;

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
