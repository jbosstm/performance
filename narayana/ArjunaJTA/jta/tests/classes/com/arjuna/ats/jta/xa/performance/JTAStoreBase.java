/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package com.arjuna.ats.jta.xa.performance;

import com.arjuna.ats.arjuna.common.CoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;

import jakarta.transaction.TransactionManager;

public class JTAStoreBase {
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

    public boolean jtaTest() {
        try {
            tm.begin();

            tm.getTransaction().enlistResource(resource1);
            tm.getTransaction().enlistResource(resource2);

            tm.commit();
        } catch(Exception e) {
            System.err.printf("JTAStoreTests#jtaTest: %s%n", e.getMessage());
            return false;
        }

        return true;
    }
}