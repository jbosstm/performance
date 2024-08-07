/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package org.jboss.narayana.rts;

import org.jboss.jbossts.star.util.TxLinkNames;
import org.jboss.logging.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;

import static org.junit.Assert.fail;

@State(Scope.Benchmark)
public class TxnTest extends TestBase {
    protected static final Logger log = Logger.getLogger(TxnTest.class);
    static final ExecutorService threadpool = Executors.newFixedThreadPool(10);

    @State(Scope.Thread)
    public static class TestState {
        public Set<Link> links;
        public String serviceRequest;
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
    public void testTxn(Blackhole bh, TestState state) throws IOException {
        bh.consume(preExeTransaction(state));
        // we want 2 txn participants so make 2 transactional requests
        bh.consume(sendRequest(state));
        bh.consume(TxnHelper.endTxn(txnClient, state.links));
    }

    private boolean preExeTransaction(TestState state) throws IOException {
        state.links = TxnHelper.beginTxn(txnClient, TXN_URL);
        Link enlistmentLink = TxnHelper.getLink(state.links, TxLinkNames.PARTICIPANT);
        state.serviceRequest = SVC_URL + "?enlistURL=" + enlistmentLink.getUri() +
                "&tid=" + Thread.currentThread().getName();
        return true;
    }

    public Set<Link> beginTx() throws IOException {
        return TxnHelper.beginTxn(txnClient, TXN_URL);
    }

    private boolean sendRequest(TestState state) throws IOException {
        for (int svcCallInc = 0; svcCallInc < TxnHelper.NO_OF_SVC_CALLS; svcCallInc++) {
            TxnHelper.sendRequest(HttpURLConnection.HTTP_OK, svcClient, state.serviceRequest + svcCallInc);
        }
        return true;
    }

    public int runTxn() throws IOException {
        try {
            log.tracef("[%s] BEGINING%n", Thread.currentThread().getName());
            Set<Link> links = beginTx();
            log.tracef("[%s] BEGUN%n", Thread.currentThread().getName());
            Link enlistmentLink = TxnHelper.getLink(links, TxLinkNames.PARTICIPANT);
            String serviceRequest = SVC_URL + "?tid=" + Thread.currentThread().getName() + "&enlistURL=" + enlistmentLink.getUri();

            // we want 2 txn participants so make 2 transactional requests
            log.tracef("[%s] SENDING%n", Thread.currentThread().getName());
            for (int i = 0; i < 1; i++)
                TxnHelper.sendRequest(HttpURLConnection.HTTP_OK, svcClient, serviceRequest);

            log.tracef("[%s] SENT%n", Thread.currentThread().getName());

            log.tracef("[%s] ENDING%n", Thread.currentThread().getName());
            int sc = TxnHelper.endTxn(txnClient, links);
            log.tracef("[%s] ENDED%n", Thread.currentThread().getName());
            return sc;
        } catch (Throwable e) {
            System.out.printf("runTxn: %s%n", e);
            return -1;
        }
    }

    @Test
    public void test() throws InterruptedException, ExecutionException {
        final Collection<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < 10; i++)
            futures.add(threadpool.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws IOException {
                    return runTxn();
                }
            }));

        for (Future future : futures)
            if (!future.isDone())
                Thread.sleep(1);

        for (Future<Integer> future : futures) {
            int res = future.get();

            if (res == -1)
                fail();
        }

        threadpool.shutdown();
    }

}