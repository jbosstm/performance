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

import bitronix.tm.recovery.RecoveryException;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.common.*;
import bitronix.tm.resource.jdbc.PoolingDataSource;
import bitronix.tm.resource.jdbc.lrc.LrcXADataSource;
import com.arjuna.ats.jta.xa.performance.XAResourceImpl;
import io.narayana.perf.product.btm.BtmXAResourceHolderState;
import io.narayana.perf.product.btm.BtmResourceBean;
import io.narayana.perf.product.btm.BtmXAResourceProducer;

import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

public class BitronixWorker<Void> extends ProductWorker<Void> {

    public BitronixWorker(ProductInterface prod) {
        super(prod);
    }

    private TransactionManager getTM() {
        return prod.getTransactionManager();
    }

    private void initProps(PoolingDataSource pds, Properties properties) {
        properties.put("user", "sa");
        properties.put("password", "sa");
        properties.put("url", "jdbc:h2:tcp://localhost/~/jbpm-db;MVCC=TRUE");
        properties.put("driverClassName", "org.h2.Driver");
    }

    private void xinitProps(PoolingDataSource pds, Properties properties) {
        pds.getDriverProperties().put("user",properties.getProperty("persistence.datasource.user","sa"));
        pds.getDriverProperties().put("password",properties.getProperty("persistence.datasource.password","sa"));
        pds.getDriverProperties().put("url",properties.getProperty("persistence.datasource.url","jdbc:h2:tcp://localhost/~/jbpm-db;MVCC=TRUE"));
        pds.getDriverProperties().put("driverClassName",properties.getProperty("persistence.datasource.driverClassName","org.h2.Driver"));
    }

/*    public void testRecycleAfterSuspend() throws Exception {
        PoolingDataSource pds = new PoolingDataSource();

        pds.setClassName(LrcXADataSource.class.getName());
        pds.setUniqueName("lrc-pds");
        pds.setMaxPoolSize(2);
        initProps(pds, pds.getDriverProperties());
//        pds.getDriverProperties().setProperty("driverClassName", org.h2.jdbcx.JdbcDataSource.class.getName());
        pds.init();

        getTM().begin();

        Connection c1 = pds.getConnection();
        c1.createStatement();
        c1.close();

        Transaction tx = getTM().suspend();

        getTM().begin();

        Connection c11 = pds.getConnection();
        c11.createStatement();
        c11.close();

        getTM().commit();


        getTM().resume(tx);

        Connection c2 = pds.getConnection();
        c2.createStatement();
        c2.close();

    }*/

    @Override
    public XAResource getXAResource() {
        return xars[xarCounter.getAndIncrement() % xars.length];
    }

/*    @Override
    public Void doWork(Void context, int batchSize, Measurement<Void> config) {
        try {super.doWork(context, batchSize, config);
            testRecycleAfterSuspend();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return context;
    }*/

    @Override
    public void init() {
        super.init();

        PoolingDataSource pds = new PoolingDataSource();

        pds.setClassName(LrcXADataSource.class.getName());
        pds.setUniqueName("lrc-pds");
        pds.setMaxPoolSize(2);
        initProps(pds, pds.getDriverProperties());
//        pds.getDriverProperties().setProperty("driverClassName", org.h2.jdbcx.JdbcDataSource.class.getName());
//        pds.init();


        btmRecovery = new BtmXAResourceHolderState(null, new BtmResourceBean());

        btmRecovery.setXar(xars[0]);
        xaResourceProducer = new BtmXAResourceProducer(btmRecovery, xars);

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

    private AtomicInteger xarCounter = new AtomicInteger(0);
    final XAResource[] xars = {new XAResourceImpl(), new XAResourceImpl()};

    BtmXAResourceHolderState btmRecovery;

    XAResourceProducer xaResourceProducer;


}