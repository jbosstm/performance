/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package io.narayana.perf.product;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

public class ProductWorker<Void> {

    ProductInterface prod;

    protected XAResource xaResource1;
    protected XAResource xaResource2;

    private TransactionManager ut;

    public ProductWorker(ProductInterface prod) {
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

    public ProductInterface getProduct() {
        return prod;
    }
}