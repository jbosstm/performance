/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package org.jboss.as.quickstarts.cmt.jts.ejb;

import java.rmi.RemoteException;

import javax.ejb.EJBObject;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

public interface InvoiceManagerEJB extends EJBObject {

	public void createInvoice(String name) throws RemoteException;
}