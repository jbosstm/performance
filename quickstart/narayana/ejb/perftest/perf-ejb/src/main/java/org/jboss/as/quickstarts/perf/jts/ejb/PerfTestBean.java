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

    private Result measureThroughput(PerfTest2BeanRemote localBean, SecondPerfBeanRemote remoteBean, Result result) {
        long now = System.currentTimeMillis();

        for (int i = 0; i < result.getNumberOfCalls(); i++) {
            try {
                transactionManager.begin();
                if (result.isEnlist())
                    transactionManager.getTransaction().enlistResource(new DummyXAResource("local"));

                if (localBean != null)
                    localBean.doWork(result.isEnlist());
                else
                    remoteBean.doWork(result.isEnlist());

                transactionManager.commit();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (transactionManager.getTransaction().getStatus() == Status.STATUS_ACTIVE)
                        transactionManager.getTransaction().rollback();
                } catch (Throwable e) {
                    // ignore
                }
            }
        }

        result.setTotalMillis(System.currentTimeMillis() - now);

        return result;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Result testBMTTxns(Result result) {
        PerfTest2BeanRemote localBean = (result.nSPort == 0 ? firstPerfBean : null);

        if (firstPerfBean == null && secondPerfBean == null)
            throw new RuntimeException("Cannot lookup remote bean");

        return measureThroughput(localBean, secondPerfBean, result);
    }
}