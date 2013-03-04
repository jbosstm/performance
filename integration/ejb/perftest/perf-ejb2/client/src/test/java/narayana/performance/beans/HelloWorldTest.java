package narayana.performance.beans;

//import narayana.performance.ejb2.EJB2Home;
//import narayana.performance.ejb2.EJB2Remote;
import org.junit.Test;

import javax.ejb.CreateException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.lang.String;
import java.lang.System;
import java.rmi.RemoteException;
import java.util.Properties;
import narayana.performance.util.Result;

public class HelloWorldTest {
    @Test
    public void verifyHelloWorld() throws NamingException, RemoteException, CreateException {
        String iiopNS = "localhost:3628";
        String jndiNS = "localhost:1199";
        boolean useIIOP = true;
        String ejb2Name = "OTSEjb2StatelessBean";
        String hwName = "HelloWorldJNDIName";

        String ns = useIIOP ? iiopNS :jndiNS;

        Properties properties = new Properties();
        properties.put("java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory");
        properties.put("java.naming.factory.url.pkgs", "=org.jboss.naming:org.jnp.interfaces");
        properties.put("java.naming.provider.url", "localhost:1099");

        Context ctx = new InitialContext(properties);
        Result result = new Result(1, 1, 1199, true, false, true, 0);

        HelloWorld bean = (HelloWorld) ctx.lookup(hwName);
        result = bean.doWork(result, useIIOP, true, iiopNS);

        System.out.printf("%s%n%s%n", Result.getHeader(new StringBuilder()), result.toString());
    }
}
