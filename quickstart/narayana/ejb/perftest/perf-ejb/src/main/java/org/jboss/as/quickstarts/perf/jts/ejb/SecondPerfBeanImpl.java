/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the 
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.quickstarts.perf.jts.ejb;

import org.jboss.as.quickstarts.perf.jts.resource.DummyXAResource;

import javax.annotation.PostConstruct;
import javax.ejb.RemoteHome;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.naming.NamingException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.rmi.RemoteException;

@RemoteHome(SecondPerfBeanHome.class)
@Stateless
public class SecondPerfBeanImpl {//implements SecondPerfBeanRemote{

	private TransactionManager transactionManager;

    @PostConstruct
    public void postConstruct() {
        try {
            transactionManager = Lookup.getTransactionManager();
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
    public long doWork(boolean enlist) throws RemoteException {
        if (enlist) {
            try {
                transactionManager.getTransaction().enlistResource(new DummyXAResource("subordinate"));
            } catch (RollbackException e) {
                throw new RuntimeException("Transaction error", e);
            } catch (SystemException e) {
                throw new RuntimeException("Transaction error", e);
            }
        }

        return 0;
    }

    @TransactionAttribute(TransactionAttributeType.NEVER)
    public long doWork() throws RemoteException {
        return 0;
    }
}
