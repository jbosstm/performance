/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package narayana.performance.ejb;

import narayana.performance.util.DummyXAResource;
import narayana.performance.util.ResourceEnlister;
import narayana.performance.util.Result;

import java.rmi.RemoteException;

import javax.annotation.Resource;
import javax.ejb.RemoteHome;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

@RemoteHome(WorkerEJBHome.class)
@Stateless
public class WorkerEJBImpl {
    @Resource(lookup = "java:jboss/TransactionManager")
    private TransactionManager transactionManager;

    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public Result doWork(Result opts) throws RemoteException {
        ResourceEnlister.enlistResources(transactionManager, opts, "subordinate");

//        opts.setInfo(this.getClass().getName());
//        System.out.println("request");
        return opts;
    }
}