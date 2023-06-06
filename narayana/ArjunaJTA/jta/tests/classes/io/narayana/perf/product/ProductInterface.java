/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package io.narayana.perf.product;

import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAResource;

interface ProductInterface<T> {
    void init();

    void fini();

    UserTransaction getUserTransaction() throws SystemException;

    TransactionManager getTransactionManager();

    String getName();

    XAResource getXAResource();
}