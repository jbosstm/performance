package org.jboss.narayana.performance.jts.client;

import io.narayana.perf.Measurement;
import io.narayana.perf.Worker;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import javax.naming.NamingException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

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

    @EJB(lookup = "corbaname:iiop:localhost:3728#jts/FirstEJBImpl")
    private FirstEJBHome firstEJBHome;

    @EJB(lookup = "corbaname:iiop:localhost:3828#jts/SecondEJBImpl")
    private SecondEJBHome secondEJBHome;

    @GET
    @Produces(MediaType.APPLICATION_XML)
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public TestResult execute(@QueryParam("numberOfThreads") int numberOfThreads,
            @QueryParam("numberOfCalls") int numberOfCalls, @QueryParam("batchSize") int batchSize) throws NamingException {

        LOG.infov("Received test executor request. numberOfThreads={0}, numberOfCalls={1}, batchSize={2}.",
                numberOfThreads, numberOfCalls, batchSize);

        final Measurement<String> measurement = new Measurement<String>(numberOfThreads, numberOfCalls, batchSize);
        final Worker<String> worker = new TestWorkerImpl(firstEJBHome, secondEJBHome);

        LOG.infov("Starting test execution.");

        measurement.measure(worker, worker);
        final TestResult testResult = new TestResult(measurement);

        LOG.infov("Completed test execution: {0}", testResult);

        return testResult;
    }

}
