/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package io.narayana.perf.product.btm;

import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.resource.common.ResourceBean;
import bitronix.tm.resource.common.XAResourceHolder;

import javax.transaction.xa.XAResource;

public class BtmXAResourceHolderState extends XAResourceHolderState {
    private XAResource xar;
    private ResourceBean bean;

    @Override
    public String getUniqueName() {
        return "bitronix";
    }

    public BtmXAResourceHolderState(XAResourceHolder resourceHolder, ResourceBean bean) {
        super(resourceHolder, bean);
        this.bean = bean;
    }

    public void setXar(XAResource xar) {
        this.xar = xar;
    }

    @Override
    public XAResource getXAResource() {
        return xar;
    }

    public ResourceBean getBean() {
        return bean;
    }
}