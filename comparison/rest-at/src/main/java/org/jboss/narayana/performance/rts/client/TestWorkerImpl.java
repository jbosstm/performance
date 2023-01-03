package org.jboss.narayana.performance.rts.client;

import io.narayana.perf.Measurement;
import io.narayana.perf.Worker;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Response;

import org.jboss.jbossts.star.util.TxLinkNames;
import org.jboss.jbossts.star.util.TxSupport;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class TestWorkerImpl implements Worker<String> {

    private static final Logger LOG = Logger.getLogger(TestWorkerImpl.class);

    private final String firstServiceUrl;

    private final String secondServiceUrl;

    private final String coordinatorUrl;

    public TestWorkerImpl(final String firstServiceUrl, final String secondServiceUrl, final String coordinatorUrl) {
        this.firstServiceUrl = firstServiceUrl;
        this.secondServiceUrl = secondServiceUrl;
        this.coordinatorUrl = coordinatorUrl;
    }

    @Override
    public String doWork(final String context, final int batchSize, final Measurement<String> measurement) {
        for (int i = 0; i < batchSize; i++) {
            executeIteration();
        }

        return null;
    }

    @Override
    public void finishWork(Measurement<String> measurement) {
    }

    @Override
    public void init() {

    }

    @Override
    public void fini() {

    }

    private void executeIteration() {
        // Cannot use single instance per object, since it is not thread-safe.
        final TxSupport txSupport = new TxSupport(coordinatorUrl);

        try {
            txSupport.startTx();
            invokeService(firstServiceUrl, txSupport);
            invokeService(secondServiceUrl, txSupport);
            txSupport.commitTx();
        } catch (final Throwable t) {
            LOG.warnv(t, "Failure during one of the executions. Rolling back the transaction: ", txSupport.getTxnUri());
            txSupport.rollbackTx();
        }
    }

    private void invokeService(final String serviceUrl, final TxSupport txSupport) throws Exception {
        final Client client = ClientBuilder.newClient();
        final Link participantLink = Link.fromUri(txSupport.getTxnUri()).rel(TxLinkNames.PARTICIPANT)
                .title(TxLinkNames.PARTICIPANT).build();

        final Response response = client.target(serviceUrl).request().header("link", participantLink).post(null);

        if (response.getStatus() != 204) {
            throw new Exception("Unexpected response code recieved from the service: " + serviceUrl + ". "
                    + "Response code: " + response.getStatus());
        }
    }

}
