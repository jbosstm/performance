/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package io.narayana.perf.product.btm;

import bitronix.tm.resource.common.ResourceBean;

public class BtmResourceBean extends ResourceBean {
    @Override
    public String getUniqueName() {
        return "bitronix";
    }
}