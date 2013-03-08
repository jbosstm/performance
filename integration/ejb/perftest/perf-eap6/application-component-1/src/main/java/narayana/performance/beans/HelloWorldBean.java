package narayana.performance.beans;

import narayana.performance.ejb.WorkerEJB;
import narayana.performance.ejb.WorkerEJBHome;
import narayana.performance.util.Result;

import javax.annotation.Resource;
import javax.ejb.*;
import javax.transaction.TransactionManager;
import java.rmi.RemoteException;

@Stateless
@Remote(HelloWorld.class)
public class HelloWorldBean implements HelloWorld {
    @EJB(lookup = "corbaname:iiop:localhost:3628#perf-test/WorkerEJBImpl")
    private WorkerEJBHome workerEJBHome;

    @Resource(lookup = "java:jboss/TransactionManager")
    private TransactionManager transactionManager;

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Result doWork(Result opts) {
        try {
            final WorkerEJB workerEJB = workerEJBHome.create();
            Measurement mb = new Measurement(transactionManager, opts) {
                @Override
                protected Result doWork(Result opts) throws RemoteException {
                    return workerEJB.doWork(opts);
                }
            };

            return mb.call();
        } catch (RemoteException e) {
            opts.setErrorCount(opts.getNumberOfCalls());
            opts.setInfo(e.getMessage());

            return opts;
        }
    }
}
