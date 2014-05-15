package org.jboss.narayana.performance.xts.client;

import io.narayana.perf.PerformanceTester;
import io.narayana.perf.Result;
import io.narayana.perf.Worker;

import javax.ws.rs.DefaultValue;
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
    public TestResult execute(@DefaultValue("1") @QueryParam("threadCount") int threadCount,
            @DefaultValue("10") @QueryParam("numberOfCalls") int numberOfCalls,
            @DefaultValue("10") @QueryParam("maxThreads") int maxThreads,
            @DefaultValue("10") @QueryParam("batchSize") int batchSize) {

        LOG.infov("Received test executor request. threadCount={0}, numberOfCalls={1}, maxThreads={2}, batchSize={3}.",
                threadCount, numberOfCalls, maxThreads, batchSize);

        final PerformanceTester<String> tester = new PerformanceTester<String>(maxThreads, batchSize);
        final Worker<String> worker = new TestWorkerImpl();

        final Result<String> options = new Result<String>(threadCount, numberOfCalls);

        LOG.infov("Starting test execution.");

        final Result<String> result = tester.measureThroughput(worker, options);
        final TestResult testResult = new TestResult(result);

        LOG.infov("Completed test execution: {0}", testResult);

        return testResult;
    }

}
