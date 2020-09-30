/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import javax.transaction.xa.XAException;
import com.atomikos.datasource.RecoverableResource;
import com.atomikos.datasource.ResourceException;
import com.atomikos.icatch.RecoveryService;
import com.atomikos.recovery.PendingTransactionRecord;
import com.atomikos.datasource.xa.XATransactionalResource;
import com.atomikos.datasource.xa.XidFactory;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import java.util.Collection;

public class AomikosXAResource2 extends XATransactionalResource implements com.atomikos.datasource.RecoverableResource, XAResource {

    public AomikosXAResource2(String uniqueResourceName) {
        super(uniqueResourceName);
    }

    public AomikosXAResource2(String uniqueResourceName, XidFactory factory) {
        super(uniqueResourceName, factory);
    }


    @Override
    public XAResource refreshXAConnection() throws ResourceException {
        return new AomikosXAResource2(String.valueOf((long)(Math.random() * 100000000)));
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
    public boolean hasMoreToRecover() {
        return super.hasMoreToRecover();
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
