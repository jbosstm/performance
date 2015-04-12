/*
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags.
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
 */
package com.arjuna.ats.jta.xa.performance;

import com.arjuna.ats.arjuna.common.CoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;

import javax.transaction.TransactionManager;

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
