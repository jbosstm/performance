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

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

public class ProductWorkerJakarta<Void> {

    ProductInterfaceJakarta prod;

    protected XAResource xaResource1;
    protected XAResource xaResource2;

    private TransactionManager ut;

    public ProductWorkerJakarta(ProductInterfaceJakarta prod) {
        this.prod = prod;
        xaResource1 = prod.getXAResource();
        xaResource2 = prod.getXAResource();
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
        try {
            ut.setTransactionTimeout(ProductComparison.TX_TIMEOUT);
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }

    public void fini() {
        prod.fini();
    }

    public String getName() {
        return prod.getName();
    }

    public ProductInterfaceJakarta getProduct() {
        return prod;
    }
}
