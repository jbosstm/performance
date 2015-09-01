package org.jboss.narayana.jts;

import java.io.File;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.narayana.performance.common.test.AbstractTestCase;
import org.jboss.narayana.performance.common.test.TestResult;
import org.jboss.narayana.performance.common.xa.DummyXAResource;
import org.jboss.narayana.performance.jts.client.TestExecutor;
import org.jboss.narayana.performance.jts.client.TestWorkerImpl;
import org.jboss.narayana.performance.jts.service.AbstractEJB;
import org.jboss.narayana.performance.jts.service.first.FirstEJB;
import org.jboss.narayana.performance.jts.service.first.FirstEJBHome;
import org.jboss.narayana.performance.jts.service.first.FirstEJBImpl;
import org.jboss.narayana.performance.jts.service.second.SecondEJB;
import org.jboss.narayana.performance.jts.service.second.SecondEJBHome;
import org.jboss.narayana.performance.jts.service.second.SecondEJBImpl;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@RunWith(Arquillian.class)
public class TestCase extends AbstractTestCase {

    private static final String EXECUTOR_URL = "http://127.0.0.1:8080/" + CLIENT_DEPLOYMENT_NAME + "/"
            + TestExecutor.RESOURCE_PATH;

    @Deployment(name = CLIENT_DEPLOYMENT_NAME, managed = false, testable = false)
    @TargetsContainer(CLIENT_CONTAINER_NAME)
    public static Archive<?> getClientDeployment() {
        final WebArchive archive = ShrinkWrap
                .create(WebArchive.class, CLIENT_DEPLOYMENT_NAME + ".war")
                .addPackage("io.narayana.perf")
                .addClasses(TestExecutor.class, TestWorkerImpl.class, FirstEJB.class, FirstEJBHome.class,
                        SecondEJB.class, SecondEJBHome.class, TestResult.class)
                .addAsWebInfResource(new File("src/test/resources/web.xml"), "web.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

        return archive;
    }

    @Deployment(name = FIRST_SERVICE_DEPLOYMENT_NAME, managed = false, testable = false)
    @TargetsContainer(FIRST_SERVICE_CONTAINER_NAME)
    public static Archive<?> getFirstServiceDeployment() {
        final WebArchive archive = ShrinkWrap
                .create(WebArchive.class, FIRST_SERVICE_DEPLOYMENT_NAME + ".war")
                .addClasses(FirstEJB.class, FirstEJBHome.class, FirstEJBImpl.class, AbstractEJB.class,
                        DummyXAResource.class)
                .addAsWebInfResource(new File("src/test/resources/first-jboss-ejb3.xml"), "jboss-ejb3.xml");

        return archive;
    }

    @Deployment(name = SECOND_SERVICE_DEPLOYMENT_NAME, managed = false, testable = false)
    @TargetsContainer(SECOND_SERVICE_CONTAINER_NAME)
    public static Archive<?> getSecondServiceDeployment() {
        final WebArchive archive = ShrinkWrap
                .create(WebArchive.class, SECOND_SERVICE_DEPLOYMENT_NAME + ".war")
                .addClasses(SecondEJB.class, SecondEJBHome.class, SecondEJBImpl.class, AbstractEJB.class,
                        DummyXAResource.class)
                .addAsWebInfResource(new File("src/test/resources/second-jboss-ejb3.xml"), "jboss-ejb3.xml");

        return archive;
    }

    @Before
    public void before() {
        startContainers();
        setNodeIdentifier(CLIENT_CONTROLLER, "101");
        setNodeIdentifier(FIRST_SERVICE_CONTROLLER, "102");
        setNodeIdentifier(SECOND_SERVICE_CONTROLLER, "103");
        enableJTS(CLIENT_CONTROLLER);
        enableJTS(FIRST_SERVICE_CONTROLLER);
        enableJTS(SECOND_SERVICE_CONTROLLER);

        if (System.getProperty("trace") != null) {
            enableTraceLogs(CLIENT_CONTROLLER);
            enableTraceLogs(FIRST_SERVICE_CONTROLLER);
            enableTraceLogs(SECOND_SERVICE_CONTROLLER);
        } else {
            enableOnlyWarningLogs(CLIENT_CONTROLLER);
            enableOnlyWarningLogs(FIRST_SERVICE_CONTROLLER);
            enableOnlyWarningLogs(SECOND_SERVICE_CONTROLLER);
        }

        restartContainers();
        deploy();
    }

    @After
    public void after() {
        try {
            undeploy();
        } finally {
            stopContainers();
        }
    }

    /**
     * Parameters restrictions: threadCount <= maxThreads; numberOfCalls >=
     * batchSize; numberOfCalls / batchSize >= threadCount
     */
    @Test
    public void test() {
        final Client client = ClientBuilder.newClient();
        final TestResult result = client.target(EXECUTOR_URL).queryParam("numberOfThreads", numberOfThreads)
                .queryParam("numberOfCalls", numberOfCalls).queryParam("batchSize", batchSize).request()
                .get(TestResult.class);

        Assert.assertNotNull(result);
        exportTestResults(result);
    }

}
