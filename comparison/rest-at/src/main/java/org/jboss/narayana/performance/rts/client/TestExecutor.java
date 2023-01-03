package org.jboss.narayana.performance.rts.client;

import io.narayana.perf.Measurement;
import io.narayana.perf.Worker;

import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.jboss.logging.Logger;
import org.jboss.narayana.performance.common.test.TestResult;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@Path(TestExecutor.RESOURCE_PATH)
public class TestExecutor {

    public static final String RESOURCE_PATH = "testExecutor";

    public static final Logger LOG = Logger.getLogger(TestExecutor.class);

    @GET
    @Produces(MediaType.APPLICATION_XML)
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public TestResult execute(@QueryParam("numberOfThreads") int numberOfThreads,
            @QueryParam("numberOfCalls") int numberOfCalls, @QueryParam("batchSize") int batchSize,
            @QueryParam("firstServiceUrl") String firstServiceUrl,
            @QueryParam("secondServiceUrl") String secondServiceUrl,
            @QueryParam("coordinatorUrl") String coordinatorUrl) {

        LOG.infov("Received test executor request. threadCount={0}, numberOfCalls={1}, batchSize={2}, "
                + "firstServiceUrl={3}, secondServiceUrl={4}, coordinatorUrl={5}.", numberOfThreads, numberOfCalls,
                batchSize, firstServiceUrl, secondServiceUrl, coordinatorUrl);

        final Measurement<String> measurement = new Measurement<String>(numberOfThreads, numberOfCalls, batchSize);
        final Worker<String> worker = new TestWorkerImpl(firstServiceUrl, secondServiceUrl, coordinatorUrl);

        LOG.infov("Starting test execution.");

        measurement.measure(worker, worker);
        final TestResult testResult = new TestResult(measurement);

        LOG.infov("Completed test execution: {0}", testResult);

        return testResult;
    }

}
