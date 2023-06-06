/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package io.narayana.perf.product.btm;

import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.recovery.RecoveryException;
import bitronix.tm.resource.common.XAResourceProducer;
import bitronix.tm.resource.common.XAResourceHolder;
import bitronix.tm.resource.common.ResourceBean;
import bitronix.tm.resource.common.XAStatefulHolder;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.transaction.xa.XAResource;

public class BtmXAResourceProducer implements XAResourceProducer {
    private BtmXAResourceHolderState btmRecovery;
    private XAResourceHolder[] xarHolders;

    public BtmXAResourceProducer(BtmXAResourceHolderState btmRecovery, XAResource ... xars) {
        this.btmRecovery = btmRecovery;
        this.xarHolders = new XAResourceHolder[xars.length];

        for (int i = 0; i < xars.length; i++) {
            xarHolders[i] = new BtmXAResourceHolder(xars[i], btmRecovery.getBean());
        }
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
        for (XAResourceHolder holder : xarHolders)
            if (xaResource.equals(holder.getXAResource()))
                return holder;

        return null;
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