/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package com.arjuna.ats.jta.xa.performance;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.internal.arjuna.objectstore.jdbc.JDBCStore;
import com.arjuna.ats.internal.arjuna.objectstore.jdbc.accessors.DynamicDataSourceJDBCAccess;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;

@State(Scope.Benchmark)
public class JDBCStoreBenchmark extends JTAStoreBase {
    @Setup(Level.Trial)
    @BeforeClass
    public static void setup() throws CoreEnvironmentBeanException {
        String storeType = JDBCStore.class.getName();
        String jdbcAccess = DynamicDataSourceJDBCAccess.class.getName() + ";ClassName=org.h2.jdbcx.JdbcDataSource;URL=jdbc:h2:./h2/JBTMDB;User=sa;Password=sa";

        JTAStoreBase.setup(storeType);
        ObjectStoreEnvironmentBean defaultStore = BeanPopulator.getDefaultInstance(ObjectStoreEnvironmentBean.class);
        ObjectStoreEnvironmentBean stateStore = BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "stateStore");
        ObjectStoreEnvironmentBean communicationStore = BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "communicationStore");

        initStore(defaultStore, storeType, jdbcAccess, "Action", true);
        initStore(stateStore, storeType, jdbcAccess, "stateStore", true);
        initStore(communicationStore, storeType, jdbcAccess, "Communication", true);
    }

    private static void initStore(ObjectStoreEnvironmentBean store, String type, String connectionDetails, String tablePrefix, boolean dropTable) {
        store.setObjectStoreType(type);
        store.setJdbcAccess(connectionDetails);
        store.setTablePrefix(tablePrefix);
        store.setDropTable(dropTable);
    }

    @Test
    @Benchmark
    public void testJDBCStore(Blackhole bh) {
        bh.consume(super.jtaTest());
    }
}