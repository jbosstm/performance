package org.jboss.as.quickstarts.perf.jts.ejb;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.TransactionManager;
import java.util.Properties;

/**
 * // TODO: Document this
 *
 * @author mmusgrov
 * @since 4.0
 */
public class Lookup {
    public static <T> T doLookup(String jndiUrl, String ... altNames) throws NamingException {
        NamingException excp = new NamingException();

        for (String name : altNames) {
            try {
                if (jndiUrl == null)
                    return (T) (new InitialContext()).lookup(name);
                else
                    return (T) (getContext(jndiUrl).lookup(name));
            } catch (NamingException e) {
                System.out.printf("%s: lookup jndi name failed\n", name);
                excp = e;
            }
        }

        throw excp;
    }

    private static Context getContext(String jndiUrl) throws NamingException {
        Properties properties = new Properties();
//TODO XXX these settings dont work on EAP6
        properties.put("java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory");
        properties.put("java.naming.factory.url.pkgs", "org.jboss.naming rg.jnp.interfaces");
        properties.put("java.naming.provider.url", jndiUrl);

        return new InitialContext(properties);
    }

    public static TransactionManager getTransactionManager() throws NamingException {
        // EAP 5 and 6 use different names
        return doLookup(null, "java:TransactionManager", "java:jboss/TransactionManager");
    }

    public static PerfTest2BeanRemote getPerfTest2BeanRemote(String jndiUrl) {
        String eap5name = "perf-ear/PerfTest2Bean/remote";
        String eap6name = "java:app/perf-ejb/PerfTest2Bean";

        try {
            return doLookup(jndiUrl, eap5name, eap6name);
        } catch (NamingException e) {
//            e.printStackTrace();
           return null;
        }
    }

    public static PerfTestBeanRemote getPerfTestBeanRemote(String jndiUrl) {
        String eap5name = "perf-ear/PerfTestBean/remote";
        String eap6name = "java:app/perf-ejb/PerfTestBean";

        try {
            return doLookup(jndiUrl, eap5name, eap6name);
        } catch (NamingException e) {
//            e.printStackTrace();
            return null;
        }
    }

    public static PerfTest2BeanRemote getPerfTest2BeanRemote() {
        return getPerfTest2BeanRemote(null);
    }

    public static PerfTestBeanRemote getPerfTestBeanRemote() {
        return getPerfTestBeanRemote(null);
    }
}

