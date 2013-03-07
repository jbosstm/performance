package narayana.performance.util;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.TransactionManager;
import java.io.IOException;
import java.util.Properties;


public class Lookup {
    public static <T> T doLookup(String jndiUrl, boolean iiop, String ... altNames) throws NamingException {
        NamingException excp = new NamingException();

        for (String name : altNames) {
            try {
                if (jndiUrl == null)
                    return (T) (new InitialContext()).lookup(name);
                else
                    return (T) (getContext(iiop, jndiUrl).lookup(name));
            } catch (NamingException e) {
                excp = e;
            }
        }

        for (String name : altNames)
            System.out.printf("%s: lookup jndi name failed\n", name);

        throw excp;
    }

    public static TransactionManager getTransactionManager() throws NamingException {
        // EAP 5 and 6 use different names
        return doLookup(null, false, "java:TransactionManager", "java:jboss/TransactionManager");
    }

    private static Context getContext(boolean iiop, String jndiUrl) throws NamingException {
        return iiop ? getIIOPContext(jndiUrl) : getContext(jndiUrl);
    }

    private static Context getIIOPContext(String endPoint) throws NamingException {
        Properties properties = new Properties();

        if (endPoint == null)
            endPoint = "localhost:3528";

        String provider = "corbaloc::ENDPOINT/NameService".replace("ENDPOINT", endPoint);

        properties.put("java.naming.factory.initial", "com.sun.jndi.cosnaming.CNCtxFactory");
        properties.put("java.naming.provider.url", provider);
        properties.put("java.naming.factory.object", "org.jboss.tm.iiop.client.IIOPClientUserTransactionObjectFactory");
        properties.put(Context.URL_PKG_PREFIXES, "org.jboss.naming.client:org.jnp.interfaces");
        properties.put("j2ee.clientName", "iiop-unit-test");

        return new InitialContext(properties);
    }

    public static <T> T lookup(String providerUrl, boolean iiop, String jndiName) {
        try {
            return doLookup(providerUrl, iiop, jndiName);
        } catch (NamingException e) {
            return null;
        }
    }

     public static Context getContext(String jndiUrl) throws NamingException {
        Properties properties = new Properties();
//TODO XXX these settings dont work on EAP6
        properties.put("java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory");
        properties.put("java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces");
        properties.put("java.naming.provider.url", jndiUrl);

        return new InitialContext(properties);
    }

    private static final String EJB2_JNDI_CORBA_URL = "corbaloc::EP/NameService";
    private static final String JNDI_URL = "jnp://EP";

    public static Context getNamingContextForEJB2(boolean useOTS, String namingProvider) throws NamingException, IOException
    {
        Properties properties = new Properties();
        String url = useOTS ? EJB2_JNDI_CORBA_URL : JNDI_URL;

        url = url.replace("EP", namingProvider);

//        System.out.println("jndi url: " + url);
        properties.setProperty(Context.PROVIDER_URL, url);

        if (useOTS) {
            org.omg.CORBA.ORB norb = org.jboss.iiop.naming.ORBInitialContextFactory.getORB();
            // if norb is not null then we are running inside the AS so make sure that its root name context
            // is used in preferenance to the one defined by Context.PROVIDER_URL
            if (norb != null)
                properties.put("java.naming.corba.orb", norb);

            properties.setProperty(Context.URL_PKG_PREFIXES, "org.jboss.iiop.naming:org.jboss.naming.client:org.jnp.interfaces");
            properties.setProperty(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.cosnaming.CNCtxFactory");
            properties.put(Context.OBJECT_FACTORIES, "org.jboss.tm.iiop.client.IIOPClientUserTransactionObjectFactory");
            return new InitialContext(properties);
        } else {
            properties.setProperty(Context.URL_PKG_PREFIXES, "org.jboss.naming.client:org.jnp.interfaces");
            properties.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.NamingContextFactory");
        }

        return new InitialContext(properties);
    }
}
