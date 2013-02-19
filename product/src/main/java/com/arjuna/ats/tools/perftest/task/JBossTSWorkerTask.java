/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and/or its affiliates,
 * and individual contributors as indicated by the @author tags.
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
 *
 * (C) 2011,
 * @author JBoss, by Red Hat.
 */
package com.arjuna.ats.tools.perftest.task;

import com.arjuna.ats.arjuna.common.Environment;
import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.arjuna.coordinator.TxStats;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple;
import com.arjuna.ats.internal.jta.transaction.arjunacore.UserTransactionImple;
import com.arjuna.ats.jta.common.jtaPropertyManager;

import java.io.PrintWriter;
import java.util.Properties;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

public class JBossTSWorkerTask extends RHWorkerTask {
    protected JBossTSWorkerTask(CyclicBarrier cyclicBarrier, AtomicInteger count, int batch_size) {
        super(cyclicBarrier, count, batch_size);
    }

    @Override
    protected void init(Properties config) {
        super.init(config);

        if (stats)
            arjPropertyManager.propertyManager.setProperty(Environment.ENABLE_STATISTICS, "YES");

        arjPropertyManager.propertyManager.setProperty(Environment.OBJECTSTORE_DIR, objectStoreDir);
        arjPropertyManager.propertyManager.setProperty(Environment.OBJECTSTORE_TYPE, objectStoreType);

        if (jts) {
            jtaPropertyManager.propertyManager.setProperty(com.arjuna.ats.jta.common.Environment.JTA_TM_IMPLEMENTATION,
                    com.arjuna.ats.internal.jta.transaction.jts.TransactionManagerImple.class.getName());
            jtaPropertyManager.propertyManager.setProperty(com.arjuna.ats.jta.common.Environment.JTA_UT_IMPLEMENTATION,
                    com.arjuna.ats.internal.jta.transaction.jts.UserTransactionImple.class.getName());
        }

        postInit();
    }

    @Override
    protected void fini() {
        super.fini();

        validateRun(TxStats.numberOfCommittedTransactions(), TxStats.numberOfAbortedTransactions());
    }

    @Override
    protected String getName() {
        return "JBossTS";
    }
}
