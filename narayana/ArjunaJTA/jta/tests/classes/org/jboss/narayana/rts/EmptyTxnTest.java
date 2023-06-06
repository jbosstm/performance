/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
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

    @State(Scope.Thread)
    public static class TestState {
        public Set<Link> links;
    }

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
    public void testEmptyTxn(Blackhole bh, TestState state) throws IOException {
        bh.consume(beginTx(state));
        // make two service calls for comparison with RTSTests#testTxn
        bh.consume(sendRequest());
        bh.consume(TxnHelper.endTxn(txnClient, state.links));
    }

    public boolean beginTx(TestState state) throws IOException {
        state.links = TxnHelper.beginTxn(txnClient, TXN_URL);
        return true;
    }

    private boolean sendRequest() throws IOException {
        for (int i = 0; i < TxnHelper.NO_OF_SVC_CALLS; i++) {
            TxnHelper.sendRequest(HttpURLConnection.HTTP_OK, svcClient, SVC_URL);
        }
        return true;
    }
}