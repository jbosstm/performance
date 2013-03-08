package narayana.performance.util;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

public class ResourceEnlister {
    public static int enlistResources(TransactionManager transactionManager, Result opts, String resourceName) {
        if (transactionManager == null)
            throw new IllegalArgumentException("enlistResources() requires a non null transaction manager");

        try {
            for (int i = opts.getEnlist(); i > 0; i--)
                transactionManager.getTransaction().enlistResource(
                        new DummyXAResource(resourceName + i, opts.getPrepareDelay()));

            return opts.getEnlist();
        } catch (RollbackException e) {
            throw new RuntimeException("Transaction error", e);
        } catch (SystemException e) {
            throw new RuntimeException("Transaction error", e);
        }
    }
}
