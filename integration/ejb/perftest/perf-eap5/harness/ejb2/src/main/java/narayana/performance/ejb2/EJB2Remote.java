/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package narayana.performance.ejb2;

import narayana.performance.util.Result;

import java.rmi.RemoteException;

public interface EJB2Remote extends javax.ejb.EJBObject
{
    public Result doWork(Result opts) throws RemoteException;
//    public Result doWork2(Result opts, boolean iiop, boolean ejb2, String namingProvider) throws RemoteException;
}