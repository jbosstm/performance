package org.jboss.narayana.performance.jts.client;

import io.narayana.perf.PerformanceTester;
import io.narayana.perf.Result;
import io.narayana.perf.Worker;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.NamingException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.jboss.logging.Logger;
import org.jboss.narayana.performance.common.test.TestResult;
import org.jboss.narayana.performance.jts.service.first.FirstEJBHome;
import org.jboss.narayana.performance.jts.service.second.SecondEJBHome;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@Stateless
@Path(TestExecutor.RESOURCE_PATH)
public class TestExecutor {

    public static final String RESOURCE_PATH = "testExecutor";

    public static final Logger LOG = Logger.getLogger(TestExecutor.class);

    @EJB(lookup = "corbaname:iiop:localhost:3628#jts/FirstEJBImpl")
    private FirstEJBHome firstEJBHome;

    @EJB(lookup = "corbaname:iiop:localhost:3728#jts/SecondEJBImpl")
    private SecondEJBHome secondEJBHome;

    @GET
    @Produces(MediaType.APPLICATION_XML)
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public TestResult execute(@QueryParam("threadCount") int threadCount,
            @QueryParam("numberOfCalls") int numberOfCalls, @QueryParam("maxThreads") int maxThreads,
            @QueryParam("batchSize") int batchSize) throws NamingException {

        LOG.infov("Received test executor request. threadCount={0}, numberOfCalls={1}, maxThreads={2}, batchSize={3}.",
                threadCount, numberOfCalls, maxThreads, batchSize);

        final PerformanceTester<String> tester = new PerformanceTester<String>(maxThreads, batchSize);
        final Worker<String> worker = new TestWorkerImpl(firstEJBHome, secondEJBHome);

        final Result<String> options = new Result<String>(threadCount, numberOfCalls);

        LOG.infov("Starting test execution.");

        final Result<String> result = tester.measureThroughput(worker, options);
        final TestResult testResult = new TestResult(result);

        LOG.infov("Completed test execution: {0}", testResult);

        return testResult;
    }

}
