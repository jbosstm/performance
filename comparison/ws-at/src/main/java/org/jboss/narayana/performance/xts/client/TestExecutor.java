package org.jboss.narayana.performance.xts.client;

import io.narayana.perf.Measurement;
import io.narayana.perf.Worker;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

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
    public TestResult execute(@QueryParam("numberOfThreads") int numberOfThreads,
            @QueryParam("numberOfCalls") int numberOfCalls, @QueryParam("batchSize") int batchSize) {

        LOG.infov("Received test executor request. numberOfThreads={0}, numberOfCalls={1}, batchSize={2}.",
                numberOfThreads, numberOfCalls, batchSize);

        final Measurement<String> measurement = new Measurement<String>(numberOfThreads, numberOfCalls, batchSize);
        final Worker<String> worker = new TestWorkerImpl();

        LOG.infov("Starting test execution.");

        measurement.measure(worker, worker);
        final TestResult testResult = new TestResult(measurement);

        LOG.infov("Completed test execution: {0}", testResult);

        return testResult;
    }

}
