package narayana.performance.beans;

import narayana.performance.util.DummyXAResource;
import narayana.performance.util.ResourceEnlister;
import narayana.performance.util.Result;

import javax.transaction.Status;
import javax.transaction.TransactionManager;
import java.rmi.RemoteException;
import java.util.concurrent.Callable;

public abstract class Measurement implements Callable<Result> {
    private TransactionManager transactionManager;
    private Result opts;

    public Measurement(TransactionManager transactionManager, Result opts) {
        this.transactionManager = transactionManager;
        this.opts = opts;
    }

    abstract protected Result doWork(Result opts) throws RemoteException;

    @Override
    public Result call() throws RemoteException {
        long nCalls = opts.getNumberOfCalls();
        long now = System.currentTimeMillis();

        for (long i = 0; i < nCalls; i++) {
            boolean ok = true;

            try {
                if (opts.isTransactional()) {
                    transactionManager.begin();
                    ResourceEnlister.enlistResources(transactionManager, opts, "local");
                }

                opts = doWork(opts);

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
