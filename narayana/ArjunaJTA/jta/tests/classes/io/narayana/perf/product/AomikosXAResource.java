/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package io.narayana.perf.product;

import java.util.Collection;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.atomikos.datasource.RecoverableResource;
import com.atomikos.datasource.ResourceException;
import com.atomikos.datasource.xa.XATransactionalResource;
import com.atomikos.datasource.xa.XidFactory;
import com.atomikos.icatch.RecoveryService;
import com.atomikos.recovery.PendingTransactionRecord;

public class AomikosXAResource extends XATransactionalResource implements com.atomikos.datasource.RecoverableResource, XAResource {

    public AomikosXAResource(String uniqueResourceName) {
        super(uniqueResourceName);
    }

    public AomikosXAResource(String uniqueResourceName, XidFactory factory) {
        super(uniqueResourceName, factory);
    }


    @Override
    public XAResource refreshXAConnection() throws ResourceException {
        return new AomikosXAResource(String.valueOf((long)(Math.random() * 100000000)));
    }

    @Override
    public void setRecoveryService(RecoveryService recoveryService) throws ResourceException {
        super.setRecoveryService(recoveryService);
    }

    @Override
    public boolean recover(long paramLong, Collection<PendingTransactionRecord> paramCollection1, Collection<PendingTransactionRecord> paramCollection2) throws ResourceException {
        return super.recover(paramLong, paramCollection1, paramCollection2);
    }

    @Override
    public void close() throws ResourceException {

    }

    @Override
    public boolean isSameRM(RecoverableResource recoverableResource) throws ResourceException {
        return true;
    }

    @Override
    public boolean isClosed() {
        return super.isClosed();
    }

    @Override
    public void commit(Xid xid, boolean b) throws XAException {
    }

    @Override
    public void end(Xid xid, int i) throws XAException {
    }

    @Override
    public void forget(Xid xid) throws XAException {
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        return 0;
    }

    @Override
    public boolean isSameRM(XAResource xaResource) throws XAException {
        return true;
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        return 0;
    }

    @Override
    public Xid[] recover(int i) throws XAException {
        return new Xid[0];
    }

    @Override
    public void rollback(Xid xid) throws XAException {
    }

    @Override
    public boolean setTransactionTimeout(int i) throws XAException {
        return false;
    }

    @Override
    public void start(Xid xid, int i) throws XAException {
    }

}