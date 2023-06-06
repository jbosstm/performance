/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package narayana.performance.ejb2;

import narayana.performance.util.Lookup;
import narayana.performance.util.ResourceEnlister;
import narayana.performance.util.Result;

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import javax.transaction.TransactionManager;
import java.io.IOException;
import java.util.Properties;

public class EJB2StatelessBean implements SessionBean
{
    String msg = "";
    TransactionManager transactionManager;

    public Result doWork(Result opts) {
        ResourceEnlister.enlistResources(getTransactionManager(), opts, "subordinate");
//        opts.setInfo(msg);

        return opts;
    }

    private TransactionManager getTransactionManager() {
        if (transactionManager == null)
            try {
                transactionManager = Lookup.getTransactionManager();
            } catch (NamingException e) {
                System.err.printf("Error looking up TM: " + e.getMessage());
            }

        return transactionManager;

    }
    public void setSessionContext(SessionContext context) {}
    public void ejbCreate() {}
    public void ejbActivate() {
        getTransactionManager();
        msg = String.format("WorkBean: bindAddress=%s portBindings=%s",
                System.getProperty("jboss.bind.address", "unknown"),
                System.getProperty("jboss.service.binding.set", "unknown"));
    }
    public void ejbPassivate() {}
    public void ejbRemove() {}
}