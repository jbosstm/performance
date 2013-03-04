package narayana.performance.beans;

import narayana.performance.ejb2.EJB2Home;
import narayana.performance.ejb2.EJB2Remote;
import narayana.performance.util.Lookup;
import narayana.performance.util.Result;
import org.jboss.ejb3.annotation.IIOP;
import org.jboss.ejb3.annotation.RemoteBinding;
import org.jboss.ejb3.annotation.defaults.RemoteBindingDefaults;

import javax.annotation.PostConstruct;
import javax.ejb.*;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import javax.transaction.TransactionManager;
import java.rmi.RemoteException;

@Stateless
@Remote(HelloWorld.class)
@RemoteBinding(jndiBinding = "HelloWorldJNDIName")
//@RemoteBinding(factory= RemoteBindingDefaults.PROXY_FACTORY_IMPLEMENTATION_IOR, jndiBinding = "HelloWorldJNDIName")
@IIOP(interfaceRepositorySupported=false)


//@RemoteBindings({@RemoteBinding(factory=IORFactory.class),@RemoteBinding(factory=StatelessRemoteProxyFactory.class)})

public class HelloWorldBean implements HelloWorld {
    private TransactionManager transactionManager;
    private String msg = null;

    @PostConstruct
    public void postConstruct() {
        try {
            this.transactionManager = Lookup.getTransactionManager();
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Result doWork(Result opts, boolean iiop, boolean ejb2, String namingProvider) {
        System.out.printf("%s%n", getMsg());

        String jndiName = iiop ? "OTSEjb2StatelessBean" : "Ejb2StatelessBean";
        try {
            Object ro = Lookup.getNamingContextForEJB2(iiop, namingProvider).lookup(jndiName);

            EJB2Home home = (EJB2Home) PortableRemoteObject.narrow(ro, EJB2Home.class);

            EJB2Remote remote = home.create();

            if (remote != null) {
                opts = Result.validateOpts(opts);
                System.out.printf("Calling remote %s%n", remote.getClass().getName());
                Measurement m = new Measurement();
                return m.measureBMTThroughput(transactionManager, remote, opts);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return opts;
    }

    private String getMsg() {
        if (msg == null) {
            msg = String.format("WorkBean: bindAddress=%s portBindings=%s",
                    System.getProperty("jboss.service.binding.set", "unknown"),
                    System.getProperty("jboss.bind.address", "unknown"));
        }

        return msg;
    }
}

