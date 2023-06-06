/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package io.narayana.perf.product;

import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;
import javax.transaction.xa.XAResource;

interface ProductInterfaceJakarta<T> {
    void init();

    void fini();

    UserTransaction getUserTransaction() throws SystemException;

    TransactionManager getTransactionManager();

    String getName();

    XAResource getXAResource();
}