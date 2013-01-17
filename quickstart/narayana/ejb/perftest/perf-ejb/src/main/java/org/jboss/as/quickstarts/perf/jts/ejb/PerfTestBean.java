package org.jboss.as.quickstarts.perf.jts.ejb;

import org.jboss.as.quickstarts.perf.jts.resource.DummyXAResource;

import javax.annotation.PostConstruct;
import javax.ejb.*;
import javax.naming.NamingException;
import javax.transaction.*;
import java.rmi.RemoteException;

@Stateless
public class PerfTestBean implements PerfTestBeanRemote {
    private TransactionManager transactionManager;

    @EJB(lookup = "corbaname:iiop:localhost:3628#jts-perftest/SecondPerfBeanImpl")
    private SecondPerfBeanHome secondPerfBeanHome;
    private SecondPerfBeanRemote secondPerfBean;
    private PerfTest2BeanRemote firstPerfBean;

    @PostConstruct
    public void postConstruct() {
        try {
            this.transactionManager = Lookup.getTransactionManager();
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }

        firstPerfBean = Lookup.getPerfTest2BeanRemote(null);

        try {
            secondPerfBean = secondPerfBeanHome.create();
            //InitialContext.doLookup("corbaname:iiop:localhost:3628#jts-perftest/SecondPerfBeanImp");
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private Result measureBMTThroughput(PerfTest2BeanRemote localBean, SecondPerfBeanRemote remoteBean, Result result) {
        long nCalls = result.getNumberOfCalls();
        boolean enlist = result.isEnlist();

        for (long i = 0; i < nCalls; i++) {
            try {
                transactionManager.begin();

                if (enlist)
                    transactionManager.getTransaction().enlistResource(new DummyXAResource("local"));

                if (localBean != null)
                    localBean.doWork(enlist);
                else
                    remoteBean.doWork(enlist);

                transactionManager.commit();
            } catch (Exception e) {
                result.incrementErrorCount();
            } finally {
                try {
                    if (transactionManager.getTransaction().getStatus() == Status.STATUS_ACTIVE) {
                        result.incrementErrorCount();
                        transactionManager.getTransaction().rollback();
                    }
                } catch (Throwable e) {
                    // ignore
                }
            }
        }

        return result;
    }

    private Result measureNonTransactionalThroughput(PerfTest2BeanRemote localBean, SecondPerfBeanRemote remoteBean, Result result) {
        long nCalls = result.getNumberOfCalls();

        for (long i = 0; i < nCalls; i++) {
            boolean ok = true;

            try {
                if (localBean != null)
                    localBean.doWork();
                else
                    remoteBean.doWork();

            } catch (Exception e) {
                ok = false;
            }

            if (!ok)
                result.incrementErrorCount();
        }

        return result;
    }

    private Result measureCMTThroughput(PerfTest2BeanRemote localBean, SecondPerfBeanRemote remoteBean, Result result) {
        try {
            if (result.isEnlist())
                transactionManager.getTransaction().enlistResource(new DummyXAResource("local"));

            if (localBean != null)
                localBean.doWork(result.isEnlist());
            else
                remoteBean.doWork(result.isEnlist());
        } catch (Exception e) {
            result.incrementErrorCount();
            System.out.printf("CMT error: %s\n", e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Result testCMTTxns(Result result) {
        PerfTest2BeanRemote localBean = (result.nSPort == 0 ? firstPerfBean : null);

        return measureCMTThroughput(localBean, secondPerfBean, result);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Result testBMTTxns(Result result) {
        PerfTest2BeanRemote localBean = (result.nSPort == 0 ? firstPerfBean : null);

        if (firstPerfBean == null && secondPerfBean == null)
            throw new RuntimeException("Cannot lookup remote bean");

        long now = System.currentTimeMillis();

        if (result.isTransactional())
            measureBMTThroughput(localBean, secondPerfBean, result);
        else
            measureNonTransactionalThroughput(localBean, secondPerfBean, result);

        result.setTotalMillis(System.currentTimeMillis() - now);

        return result;
    }
}