/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package org.jboss.as.quickstarts.cmt.jts.ejb;

import java.rmi.RemoteException;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.RemoteHome;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.NamingException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.jboss.as.quickstarts.cmt.jts.resource.DummyXAResource;

@RemoteHome(CustomerManagerEJBHome.class)
@Stateless
public class CustomerManagerEJBImpl {

	@Resource(lookup = "java:jboss/TransactionManager")
	private TransactionManager transactionManager;

	@EJB(lookup = "corbaname:iiop:localhost:3628#jts-quickstart/InvoiceManagerEJBImpl")
	private InvoiceManagerEJBHome invoiceManagerHome;

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createCustomer(String name) throws RemoteException,
			SystemException, IllegalStateException, RollbackException,
			NamingException {

		try {
			// ((TransactionManager) new InitialContext()
			// .lookup("java:jboss/TransactionManager"))
			Transaction transaction = transactionManager.getTransaction();
			// System.out.println("CustomerManagerEJBImpl"
			// + transaction.getStatus());
			transaction.enlistResource(new DummyXAResource("local"));

			final InvoiceManagerEJB invoiceManager = invoiceManagerHome
					.create();
			invoiceManager.createInvoice(name);
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}
}