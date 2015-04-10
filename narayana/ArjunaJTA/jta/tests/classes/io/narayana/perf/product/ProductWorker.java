/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package io.narayana.perf.product;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import com.arjuna.ats.jta.xa.performance.XAResourceImpl;

public class ProductWorker<Void> {

    ProductInterface prod;
    
    protected XAResource xaResource1 = new XAResourceImpl();
    protected XAResource xaResource2 = new XAResourceImpl();

    private TransactionManager ut;

    public ProductWorker(ProductInterface prod) {
        this.prod = prod;
    }

    public void doWork() throws SystemException, NotSupportedException, RollbackException, HeuristicRollbackException, HeuristicMixedException {
        ut.begin();
        ut.getTransaction().enlistResource(xaResource1);
        ut.getTransaction().enlistResource(xaResource2);
        ut.commit();
    }

    public void init() {
        prod.init();
        ut = prod.getTransactionManager();
    }

    public void fini() {
        prod.fini();
    }

}
