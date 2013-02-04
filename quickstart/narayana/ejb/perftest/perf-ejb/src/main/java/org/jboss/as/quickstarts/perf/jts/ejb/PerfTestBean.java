package org.jboss.as.quickstarts.perf.jts.ejb;

import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.javasimon.StopwatchSample;
import org.javasimon.callback.CallbackSkeleton;
import org.javasimon.utils.SimonUtils;
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
    private Sampler sampler;
    private boolean sampling = false;
    private Stopwatch[] stopwatches = {
            SimonManager.getStopwatch("PerfTestBean"),
            SimonManager.getStopwatch("PerfTestBean.enlist"),
            SimonManager.getStopwatch("PerfTestBean.doWork"),
            SimonManager.getStopwatch("PerfTestBean.commit")
    };

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

        sampler = new Sampler(stopwatches, 10000L);
        sampler.setDaemon(true);
//        sampler.start();
    }

    private Split startStopwatch(Stopwatch stopwatch) {
        return sampling ? stopwatches[1].start() : null;
    }

    private void stopStopwatch(Split split) {
        if (sampling)
            split.stop();
    }

    private Result measureBMTThroughput(PerfTest2BeanRemote localBean, SecondPerfBeanRemote remoteBean, Result opts) {
        long nCalls = opts.getNumberOfCalls();
        Split[] splits = {null, null, null, null};

        sampler.setReport(true);

        for (long i = 0; i < nCalls; i++) {
            splits[0] = startStopwatch(stopwatches[0]);

            try {
                transactionManager.begin();

                if (opts.getEnlist() > 0) {
                    splits[1] = startStopwatch(stopwatches[1]) ;
                    for (int j = opts.getEnlist(); j > 0; j--) {
                        transactionManager.getTransaction().enlistResource(
                                new DummyXAResource("local" + j, opts.getPrepareDelay()));
                    }
                    stopStopwatch(splits[1]);
                }

                splits[2] = startStopwatch(stopwatches[2]) ;
                if (localBean != null)
                    localBean.doWork(opts);
                else
                    remoteBean.doWork(opts);

                stopStopwatch(splits[2]);

                splits[3]  = startStopwatch(stopwatches[3]) ;
                transactionManager.commit();
                stopStopwatch(splits[3]);
            } catch (Exception e) {
                opts.incrementErrorCount();
            } finally {
                try {
                    if (transactionManager.getTransaction().getStatus() == Status.STATUS_ACTIVE) {
                        opts.incrementErrorCount();
                        transactionManager.getTransaction().rollback();
                    }
                } catch (Throwable e) {
                    // ignore
                }
                for (Split split : splits)
                    if (split!= null && split.isRunning())
                        stopStopwatch(split);
            }
        }

        sampler.setReport(false);

        if (sampling)
            sampler.report();

        return opts;
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

    private Result measureCMTThroughput(PerfTest2BeanRemote localBean, SecondPerfBeanRemote remoteBean, Result opts) {
        try {
            for (int i = opts.getEnlist(); i > 0; i--)
                transactionManager.getTransaction().enlistResource(
                        new DummyXAResource("local" + i, opts.getPrepareDelay()));

            if (localBean != null)
                localBean.doWork(opts);
            else
                remoteBean.doWork(opts);

        } catch (Exception e) {
            opts.incrementErrorCount();
            System.out.printf("CMT error: %s\n", e.getMessage());
            e.printStackTrace();
        }

        return opts;
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