package org.jboss.as.quickstarts.perf.jts.ejb;

import org.jboss.as.quickstarts.perf.jts.resource.DummyXAResource;

import javax.ejb.RemoteHome;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

@Stateless
public class PerfTest2Bean  implements PerfTest2BeanRemote {
    private TransactionManager transactionManager;

    public PerfTest2Bean() throws NamingException {
        this.transactionManager = Lookup.getTransactionManager();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public long doWork(boolean enlist) {
        if (enlist) {
            try {
                transactionManager.getTransaction().enlistResource(new DummyXAResource("subordinate"));
            } catch (RollbackException e) {
                throw new RuntimeException("Transaction error", e);
            } catch (SystemException e) {
                throw new RuntimeException("Transaction error", e);
            }
        }

        return 0;
    }
}
