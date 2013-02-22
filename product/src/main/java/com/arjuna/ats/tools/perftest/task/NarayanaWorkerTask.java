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

import com.arjuna.ats.arjuna.common.CoordinatorEnvironmentBean;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.arjuna.coordinator.TxStats;
import com.arjuna.ats.arjuna.objectstore.StoreManager;
import com.arjuna.ats.internal.arjuna.objectstore.hornetq.HornetqJournalEnvironmentBean;
import com.arjuna.ats.internal.jts.orbspecific.recovery.RecoveryEnablement;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

public class NarayanaWorkerTask extends RHWorkerTask {
    protected NarayanaWorkerTask(CyclicBarrier cyclicBarrier, AtomicInteger count, int batch_size) {
        super(cyclicBarrier, count, batch_size);

        if (productName.length() == 0)
            productName = "Narayana XXX";
    }

    @Override
    protected void init(Properties config) {
        super.init(config);

        JTAEnvironmentBean jtaEnvironmentBean = BeanPopulator.getDefaultInstance(JTAEnvironmentBean.class);

        if (!jts) {
            jtaEnvironmentBean.setTransactionManagerClassName(com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple.class.getName());
//        jtaEnvironmentBean.setTransactionSynchronizationRegistryClassName(com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple.class.getName());
            jtaEnvironmentBean.setUserTransactionClassName(com.arjuna.ats.internal.jta.transaction.arjunacore.UserTransactionImple.class.getName());
        } else {
            jtaEnvironmentBean.setTransactionManagerClassName(com.arjuna.ats.internal.jta.transaction.jts.TransactionManagerImple.class.getName());
            jtaEnvironmentBean.setUserTransactionClassName(com.arjuna.ats.internal.jta.transaction.jts.UserTransactionImple.class.getName());
        }

        BeanPopulator.getDefaultInstance(CoordinatorEnvironmentBean.class).setEnableStatistics(stats);
        BeanPopulator.getDefaultInstance(ObjectStoreEnvironmentBean.class).setObjectStoreDir(objectStoreDir);
        BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "communicationStore").setObjectStoreDir(objectStoreDir);
        BeanPopulator.getDefaultInstance(ObjectStoreEnvironmentBean.class).setObjectStoreType(objectStoreType);

        if (objectStoreType.endsWith("HornetqObjectStoreAdaptor" )) {
            File hornetqStoreDir = new File(new File(objectStoreDir), "HornetQStore");

            try {
                BeanPopulator.getDefaultInstance(HornetqJournalEnvironmentBean.class)
                        .setStoreDir(hornetqStoreDir.getCanonicalPath());
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

        if (jts)
            new RecoveryEnablement().startRCservice();

        postInit();
    }

    @Override
    protected void fini() {
        StoreManager.shutdown();
        super.fini();

/*        TxControl.disable(true);
        RecoveryManager.manager().terminate();
        TransactionReaper.terminate(true);*/

        validateRun(TxStats.getInstance().getNumberOfCommittedTransactions(), TxStats.getInstance().getNumberOfAbortedTransactions());
    }
}
