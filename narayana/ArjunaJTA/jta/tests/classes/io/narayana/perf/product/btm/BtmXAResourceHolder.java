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
