/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package com.arjuna.ats.jta.xa.performance;

import com.arjuna.ats.arjuna.common.CoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import org.jboss.logging.Logger;

public class JTAStoreBase {
    protected static final Logger log = Logger.getLogger(JTAStoreBase.class);
    private static XAResourceImpl resource1;
    private static XAResourceImpl resource2;
    private static TransactionManager tm;

    protected static void setup(String storeType) throws CoreEnvironmentBeanException {
        BeanPopulator.getDefaultInstance(CoreEnvironmentBean.class).setNodeIdentifier("1");
        BeanPopulator.getDefaultInstance(ObjectStoreEnvironmentBean.class).setObjectStoreType(storeType);

        tm = com.arjuna.ats.jta.TransactionManager.transactionManager();
        resource1 = new XAResourceImpl();
        resource2 = new XAResourceImpl();
    }

    public boolean jtaTest() throws HeuristicRollbackException, SystemException, HeuristicMixedException, NotSupportedException, RollbackException {
        try {
            tm.begin();

            tm.getTransaction().enlistResource(resource1);
            tm.getTransaction().enlistResource(resource2);

            tm.commit();
        } catch(Exception e) {
            log.warnf("JTAStoreTests#jtaTest%n", e);
            throw e;
        }

        return true;
    }
}
