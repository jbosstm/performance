/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */


package narayana.performance.ejb2;

import javax.ejb.CreateException;
import javax.ejb.EJBHome;
import java.rmi.RemoteException;

public interface EJB2Home extends EJBHome
{
    EJB2Remote create() throws CreateException, RemoteException;
}