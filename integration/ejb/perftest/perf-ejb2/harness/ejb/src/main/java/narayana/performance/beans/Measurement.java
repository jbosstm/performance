package narayana.performance.beans;

import narayana.performance.ejb2.EJB2Remote;
import narayana.performance.util.DummyXAResource;
import narayana.performance.util.Result;

import javax.transaction.Status;
import javax.transaction.TransactionManager;

public class Measurement {

    public Result measureBMTThroughput(TransactionManager transactionManager, EJB2Remote bean, Result opts) {
        long nCalls = opts.getNumberOfCalls();
        long now = System.currentTimeMillis();

        for (long i = 0; i < nCalls; i++) {
            boolean ok = true;

            try {
                if (opts.isTransactional()) {
                    transactionManager.begin();

                    if (opts.getEnlist() > 0) {
                        for (int j = opts.getEnlist(); j > 0; j--)
                            transactionManager.getTransaction().enlistResource(
                                    new DummyXAResource("local" + j, opts.getPrepareDelay()));
                    }
                }

                opts = bean.doWork(opts);

                if (opts.isTransactional())
                    transactionManager.commit();
            } catch (Exception e) {
                ok = false;
            } finally {
                try {
                    if (opts.isTransactional() && transactionManager.getTransaction().getStatus() == Status.STATUS_ACTIVE) {
                        ok = false;
                        transactionManager.getTransaction().rollback();
                    }
                } catch (Throwable e) {
                    // ignore
                }

                if (!ok)
                    opts.incrementErrorCount();
            }
        }

        opts.setTotalMillis(System.currentTimeMillis() - now);

        return opts;
    }

}
