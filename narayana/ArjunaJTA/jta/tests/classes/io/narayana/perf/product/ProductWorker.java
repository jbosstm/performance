/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import com.arjuna.ats.jta.xa.performance.XAResourceImpl;

import javax.transaction.*;
import javax.transaction.xa.XAResource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class ProductWorker<Void> {

    ProductInterface prod;

    public ProductWorker(ProductInterface prod) {
        this.prod = prod;
    }

    public XAResource getXAResource() {
        return new XAResourceImpl();
    }

    public void doWork() throws SystemException, NotSupportedException, RollbackException, HeuristicRollbackException, HeuristicMixedException {
        TransactionManager ut = prod.getTransactionManager();

        ut.begin();
        ut.getTransaction().enlistResource(getXAResource());
        ut.getTransaction().enlistResource(getXAResource());
        ut.commit();
    }

    protected void executeSql(Connection c, String sql, boolean ignoreErrors) throws SQLException {
        Statement s = c.createStatement();

//        statements.add(s);

        try {
            s.execute(sql);
        } catch (SQLException e) {
            if (!ignoreErrors)
                throw e;
        }
    }

    public void init() {
        prod.init();
/*        DataSource ds = prod.getDataSource();

        if (ds != null) {
            try {
                Connection c = ds.getConnection();

                executeSql(c, "CREATE SCHEMA " + prod.getName(), true);
                executeSql(c, SQLT1.replace("$DB", prod.getName()), true);
                executeSql(c, SQLT2.replace("$DB", prod.getName()), true);
            } catch (SQLException e) {
            }
        }*/
    }

    public void fini() {
        prod.fini();
    }

    private static String SQLT1 = "create table $DB.TEST(id int, value varchar(40))";
    private static String SQLT1_I = "insert into $DB.TEST values (?, ?)";
    private static String SQLT2 = "create table $DB.RESULT(product varchar(64), dbvendor varchar(32), pass int, fail int, maxt int, mint int, avgt int, count int)";
    private static String SQLT2_I = "insert into $DB.RESULT values (?, ?, ?, ?, ?, ?, ?, ?)";

}
