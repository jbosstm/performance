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
import org.junit.BeforeClass;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

import jakarta.ws.rs.core.Link;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Set;

@State(Scope.Benchmark)
public class EmptyTxnTest extends TestBase {

    static Set<Link> links;

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

    @Benchmark
    public void testEmptyTxn(Blackhole bh) throws IOException {
        bh.consume(beginTx());
        // make two service calls for comparison with RTSTests#testTxn
        bh.consume(sendRequest());
        bh.consume(TxnHelper.endTxn(txnClient, links));
    }

    public boolean beginTx() throws IOException {
        links = TxnHelper.beginTxn(txnClient, TXN_URL);
        return true;
    }

    private boolean sendRequest() throws IOException {
        for (int i = 0; i < TxnHelper.NO_OF_SVC_CALLS; i++) {
            TxnHelper.sendRequest(HttpURLConnection.HTTP_OK, svcClient, SVC_URL);
        }
        return true;
    }
}
