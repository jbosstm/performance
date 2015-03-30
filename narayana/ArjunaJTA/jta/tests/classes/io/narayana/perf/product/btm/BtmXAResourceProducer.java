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
package io.narayana.perf.product.btm;

import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.recovery.RecoveryException;
import bitronix.tm.resource.common.*;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.transaction.xa.XAResource;

public class BtmXAResourceProducer implements XAResourceProducer {
    private XAResource xar;
    private BtmXAResourceHolderState btmRecovery;
    XAResourceHolder xarHolder;

    public BtmXAResourceProducer(XAResource xar, BtmXAResourceHolderState btmRecovery) {
        this.xar = xar;
        this.btmRecovery = btmRecovery;
        this.xarHolder = new BtmXAResourceHolder(xar, btmRecovery.getBean());
    }

    @Override
    public String getUniqueName() {
        return "bitronix";
    }

    @Override
    public XAResourceHolderState startRecovery() throws RecoveryException {
        return btmRecovery;
    }

    @Override
    public void endRecovery() throws RecoveryException {

    }

    @Override
    public void setFailed(boolean b) {

    }

    @Override
    public XAResourceHolder findXAResourceHolder(XAResource xaResource) {
        if (!xaResource.equals(xar))
            return null;

        return xarHolder;
    }

    @Override
    public void init() {

    }

    @Override
    public void close() {

    }

    @Override
    public XAStatefulHolder createPooledConnection(Object o, ResourceBean resourceBean) throws Exception {
        return null;
    }

    @Override
    public Reference getReference() throws NamingException {
        return null;
    }
}
