package com.arjuna.ats.jta.xa.performance;

import com.arjuna.ats.arjuna.common.CoordinatorEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.internal.arjuna.objectstore.VolatileStore;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;
import org.openjdk.jmh.annotations.Benchmark;

import javax.transaction.TransactionManager;

public class JTAStoreTests {
    private static TransactionManager tm;

    public JTAStoreTests() {
        tm = com.arjuna.ats.jta.TransactionManager.transactionManager();
        try {
            BeanPopulator.getDefaultInstance(CoreEnvironmentBean.class).setNodeIdentifier("0");
        } catch (CoreEnvironmentBeanException e) {
            throw new RuntimeException(e);
        }
        BeanPopulator.getDefaultInstance(ObjectStoreEnvironmentBean.class).setObjectStoreType(VolatileStore.class.getName());
    }

    @Benchmark
    public void jtaTest() {
        try {
            tm.begin();

            tm.getTransaction().enlistResource(new XAResourceImpl());
            tm.getTransaction().enlistResource(new XAResourceImpl());

            tm.commit();
        } catch(Exception e) {
        }
    }
}