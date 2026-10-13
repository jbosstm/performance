/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package com.arjuna.ats.jta.xa.performance;

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.Blackhole;

import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.internal.arjuna.objectstore.jdbc.JDBCStore;
import com.arjuna.ats.internal.arjuna.objectstore.jdbc.accessors.DynamicDataSourceJDBCAccess;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;

@State(Scope.Benchmark)
public class JDBCStoreBenchmark extends JTAStoreBase {
    static final String MAX_OPEN_TRANSACTIONS_PROPERTY = "h2.maxOpenTransactions"; // H2 property
    static final int MAX_OPEN_TRANSACTIONS = 255; // matches the H2 default

    // By default, H2 only allows MAX_OPEN_TRANSACTIONS to be running. Since the thread count for the benchmark
    // determines the number of concurrent transactions it must not exceed what H2 allows, the thread count
    // must be read and used to configure H2 before running setup which starts H2 during the initStore calls.
    // Note we cannot use getThreadCountFromProperties since that only checks the JMHARGS env variable.
    @Setup(Level.Trial)
    public void setupTrial(BenchmarkParams params) throws CoreEnvironmentBeanException {
        int threadCount = params.getThreads();

        // MAX_OPEN_TRANSACTIONS_PROPERTY takes precedence allowing it to be overridden from the command line
        if (threadCount > MAX_OPEN_TRANSACTIONS && System.getProperty(MAX_OPEN_TRANSACTIONS_PROPERTY) == null) {
            System.setProperty(MAX_OPEN_TRANSACTIONS_PROPERTY, Integer.toString(roundUp(256, threadCount)));
        }

        setup();
    }

    void setup() throws CoreEnvironmentBeanException {
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
    public void testJDBCStore(Blackhole bh) throws HeuristicRollbackException, SystemException, HeuristicMixedException, NotSupportedException, RollbackException {
        bh.consume(super.jtaTest());
    }
}