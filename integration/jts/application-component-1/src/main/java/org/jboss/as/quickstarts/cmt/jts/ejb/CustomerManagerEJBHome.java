/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package org.jboss.as.quickstarts.cmt.jts.ejb;

import java.rmi.RemoteException;

import javax.ejb.EJBHome;

public interface CustomerManagerEJBHome extends EJBHome {

    public CustomerManagerEJB create() throws RemoteException;

}