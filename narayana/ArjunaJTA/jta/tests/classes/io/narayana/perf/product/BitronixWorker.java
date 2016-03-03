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

import io.narayana.perf.product.btm.BtmResourceBean;
import io.narayana.perf.product.btm.BtmXAResourceHolderState;
import io.narayana.perf.product.btm.BtmXAResourceProducer;
import bitronix.tm.recovery.RecoveryException;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.common.XAResourceProducer;

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
