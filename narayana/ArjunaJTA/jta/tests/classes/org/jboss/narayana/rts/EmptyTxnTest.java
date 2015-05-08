/*
 * Copyright 2015, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.narayana.rts;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openjdk.jmh.annotations.*;

import javax.ws.rs.core.Link;

import java.io.IOException;
import java.net.HttpURLConnection;

import java.util.Set;

@State(Scope.Benchmark)
public class EmptyTxnTest extends TestBase {

    @Setup(Level.Trial)
    @BeforeClass
    public static void setup() throws Exception {
        TestBase.setup();
    }

    @TearDown
    @AfterClass
    public static void tearDown() throws IOException {
        TestBase.tearDown();
    }

    @Test
    @Benchmark
    public void testEmptyTxn() throws IOException {
        Set<Link> links = TxnHelper.beginTxn(txnClient, TXN_URL);
        // make two service calls for comparison with RTSTests#testTxn
        for (int i = 0; i < TxnHelper.NO_OF_SVC_CALLS; i++) {
            String val = TxnHelper.sendRequest(HttpURLConnection.HTTP_OK, svcClient, SVC_URL);
            Assert.assertEquals(TransactionAwareResource.NON_TXN_MSG, val);
        }
        TxnHelper.endTxn(txnClient, links);
    }
}
