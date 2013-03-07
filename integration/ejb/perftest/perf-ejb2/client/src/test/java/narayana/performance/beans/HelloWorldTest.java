package narayana.performance.beans;

import narayana.performance.util.Lookup;
import org.junit.Test;

import javax.ejb.CreateException;
import javax.naming.Context;
import javax.naming.NamingException;
import java.lang.String;
import java.lang.System;
import java.rmi.RemoteException;

import narayana.performance.util.Result;

public class HelloWorldTest {
    @Test
    public void verifyHelloWorld() throws NamingException, RemoteException, CreateException {
        String iiopNS = "localhost:3628";
        boolean useIIOP = true;
        String hwName = "HelloWorldJNDIName";
        Context ctx = Lookup.getContext("localhost:1099");
        HelloWorld bean = (HelloWorld) ctx.lookup(hwName);
        Result result = new Result(iiopNS, false, 1, 100, 1, useIIOP, false, true, 0, false, true);

        result = bean.doWork(result);

        System.out.printf("%s%n%s%n", Result.getHeaderAsText(new StringBuilder()), result.toString());
    }
}
