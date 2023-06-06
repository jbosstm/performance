/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
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