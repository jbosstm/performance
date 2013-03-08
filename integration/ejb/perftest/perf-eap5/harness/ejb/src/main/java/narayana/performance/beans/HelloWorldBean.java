package narayana.performance.beans;

import narayana.performance.ejb2.EJB2Home;
import narayana.performance.ejb2.EJB2Remote;
import narayana.performance.util.Lookup;
import narayana.performance.util.Result;
import org.jboss.ejb3.annotation.IIOP;
import org.jboss.ejb3.annotation.RemoteBinding;

import javax.annotation.PostConstruct;
import javax.ejb.*;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import javax.transaction.TransactionManager;
import java.rmi.RemoteException;

@Stateless
@Remote(HelloWorld.class)
@RemoteBinding(jndiBinding = "HelloWorldJNDIName")
@IIOP(interfaceRepositorySupported=false)
public class HelloWorldBean implements HelloWorld {
    private TransactionManager transactionManager;

    @PostConstruct
    public void postConstruct() {
        try {
            this.transactionManager = Lookup.getTransactionManager();
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Result doWork(Result opts) {
        String jndiName = opts.isIiop() ? "OTSEjb2StatelessBean" : "Ejb2StatelessBean";

        try {
            Object ro = Lookup.getNamingContextForEJB2(opts.isIiop(), opts.getNamingProvider()).lookup(jndiName);
            EJB2Home home = (EJB2Home) PortableRemoteObject.narrow(ro, EJB2Home.class);
            final EJB2Remote remote = home.create();

            if (remote != null) {
                Measurement mb = new Measurement(transactionManager, opts) {
                    @Override
                    protected Result doWork(Result opts) throws RemoteException {
                        return remote.doWork(opts);
                    }
                };

                return mb.call();
            }
        } catch (Exception e) {
            opts.setErrorCount(opts.getNumberOfCalls());
            opts.setInfo(e.getMessage());
        }

        return opts;
    }
}

