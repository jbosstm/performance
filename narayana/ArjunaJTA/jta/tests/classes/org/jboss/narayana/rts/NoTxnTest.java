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

import javax.ws.rs.client.Client;
import java.io.IOException;
import java.net.HttpURLConnection;

@State(Scope.Benchmark)
public class NoTxnTest {
    private static JAXRSServer server;
    private static Client client;

    static final String serviceUrl = String.format("http://%s:%d%s", "localhost", 8082, "/eg/service");

    @Setup(Level.Trial)
    @BeforeClass
    public static void setup() throws Exception {
        server = new JAXRSServer(8082);
        server.addDeployment(new TransactionAwareResource.ServiceApp(), "eg");
        client = server.createClient();
    }

    @TearDown
    @AfterClass
    public static void tearDown() throws IOException {
        client.close();
        server.stop();
    }

    @Test
    @Benchmark
    public void testNoTxn() throws IOException {
        // make four calls for comparison with RTSTests#testTxn which makes 2 REST calls for starting and stopping
        // the txn plus two service calls (assuming that they are not cached)
        for (int i = 0; i < TxnHelper.NO_OF_SVC_CALLS; i++) {
            String val = TxnHelper.sendRequest(HttpURLConnection.HTTP_OK, client, serviceUrl);

            Assert.assertEquals(TransactionAwareResource.NON_TXN_MSG, val);
        }
    }
}
