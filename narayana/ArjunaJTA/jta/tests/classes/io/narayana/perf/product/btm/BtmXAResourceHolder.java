/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package io.narayana.perf.product.btm;

import bitronix.tm.BitronixXid;
import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.resource.common.ResourceBean;
import bitronix.tm.resource.common.StateChangeListener;
import bitronix.tm.resource.common.XAResourceHolder;
import bitronix.tm.utils.Uid;

import javax.transaction.xa.XAResource;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class BtmXAResourceHolder implements XAResourceHolder {
    XAResource xar;
    ResourceBean bean;

    public BtmXAResourceHolder(XAResource xar, ResourceBean bean) {
        this.xar = xar;
        this.bean = bean;
    }

    @Override
    public XAResource getXAResource() {
        return xar;
    }

    @Override
    public Map<Uid, XAResourceHolderState> getXAResourceHolderStatesForGtrid(Uid uid) {
        return null;
    }

    @Override
    public void putXAResourceHolderState(BitronixXid bitronixXid, XAResourceHolderState xaResourceHolderState) {

    }

    @Override
    public void removeXAResourceHolderState(BitronixXid bitronixXid) {

    }

    @Override
    public boolean hasStateForXAResource(XAResourceHolder xaResourceHolder) {
        return false;
    }

    @Override
    public ResourceBean getResourceBean() {
        return bean;
    }

    @Override
    public int getState() {
        return 0;
    }

    @Override
    public void setState(int i) {

    }

    @Override
    public void addStateChangeEventListener(StateChangeListener stateChangeListener) {

    }

    @Override
    public void removeStateChangeEventListener(StateChangeListener stateChangeListener) {

    }

    @Override
    public List<XAResourceHolder> getXAResourceHolders() {
        return null;
    }

    @Override
    public Object getConnectionHandle() throws Exception {
        return null;
    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public Date getLastReleaseDate() {
        return null;
    }

}