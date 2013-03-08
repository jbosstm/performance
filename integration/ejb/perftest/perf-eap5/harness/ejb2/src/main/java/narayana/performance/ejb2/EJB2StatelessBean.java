/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 *
 * (C) 2008
 * @author JBoss Inc.
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
