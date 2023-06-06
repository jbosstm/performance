/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package org.jboss.as.quickstarts.cmt.jts.ejb;

import java.rmi.RemoteException;

import javax.annotation.Resource;
import javax.ejb.RemoteHome;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.jboss.as.quickstarts.cmt.jts.resource.DummyXAResource;

@RemoteHome(InvoiceManagerEJBHome.class)
@Stateless
public class InvoiceManagerEJBImpl {

	@Resource(lookup = "java:jboss/TransactionManager")
	private TransactionManager transactionManager;

	@Inject
	private TransactionManager tm;

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public void createInvoice(String name) throws RemoteException {
		try {
			Transaction transaction = transactionManager.getTransaction();
			// System.out.println("InvoiceManagerEJBImpl"
			// + transaction.getStatus());
			transaction.enlistResource(new DummyXAResource("subordinate"));
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}

	}
}