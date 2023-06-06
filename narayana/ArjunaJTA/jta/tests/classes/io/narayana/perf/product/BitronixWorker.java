/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package io.narayana.perf.product;

import bitronix.tm.recovery.RecoveryException;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.common.XAResourceProducer;
import io.narayana.perf.product.btm.BtmResourceBean;
import io.narayana.perf.product.btm.BtmXAResourceHolderState;
import io.narayana.perf.product.btm.BtmXAResourceProducer;

public class BitronixWorker<Void> extends ProductWorker<Void> {

    private XAResourceProducer xaResourceProducer;

    public BitronixWorker(ProductInterface prod) {
        super(prod);
    }

    @Override
    public void init() {
        super.init();

        BtmXAResourceHolderState btmRecovery = new BtmXAResourceHolderState(null, new BtmResourceBean());
        btmRecovery.setXar(xaResource1);

        xaResourceProducer = new BtmXAResourceProducer(btmRecovery, xaResource1, xaResource2);

        try {
            ResourceRegistrar.register(xaResourceProducer);
        } catch (RecoveryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void fini() {
        ResourceRegistrar.unregister(xaResourceProducer);
        super.fini();
    }
}