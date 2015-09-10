/*
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package com.arjuna.ats.jta.xa.performance;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

public class XAResourceImpl implements XAResource {
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
        return xaResource.equals(this);
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

    public String getName() {
        return String.valueOf(hashCode());
    }
}
