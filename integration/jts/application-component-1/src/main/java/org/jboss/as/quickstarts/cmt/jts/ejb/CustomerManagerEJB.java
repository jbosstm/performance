/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package org.jboss.as.quickstarts.cmt.jts.ejb;

import java.rmi.RemoteException;

import javax.ejb.EJBObject;

public interface CustomerManagerEJB extends EJBObject {

	public void createCustomer(String name) throws RemoteException;
}