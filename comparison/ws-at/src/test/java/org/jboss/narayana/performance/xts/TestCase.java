package org.jboss.narayana.performance.xts;

import java.io.File;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.narayana.performance.common.test.AbstractTestCase;
import org.jboss.narayana.performance.common.test.TestResult;
import org.jboss.narayana.performance.common.xa.DummyXAResource;
import org.jboss.narayana.performance.xts.client.TestExecutor;
import org.jboss.narayana.performance.xts.client.TestWorkerImpl;
import org.jboss.narayana.performance.xts.service.first.FirstService;
import org.jboss.narayana.performance.xts.service.first.FirstServiceClientFactory;
import org.jboss.narayana.performance.xts.service.first.FirstServiceImpl;
import org.jboss.narayana.performance.xts.service.second.SecondService;
import org.jboss.narayana.performance.xts.service.second.SecondServiceClientFactory;
import org.jboss.narayana.performance.xts.service.second.SecondServiceImpl;
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

    private static final String EXECUTOR_URL = "http://127.0.0.1:" + (HTTP_PORT + CLIENT_CONTAINER_OFFSET)
            + "/" + CLIENT_DEPLOYMENT_NAME + "/" + TestExecutor.RESOURCE_PATH;

    @Deployment(name = CLIENT_DEPLOYMENT_NAME, managed = false, testable = false)
    @TargetsContainer(CLIENT_CONTAINER_NAME)
    public static Archive<?> getClientDeployment() {
        final WebArchive archive = ShrinkWrap
                .create(WebArchive.class, CLIENT_DEPLOYMENT_NAME + ".war")
                .addPackage("io.narayana.perf")
                .addClasses(TestExecutor.class, TestWorkerImpl.class, TestResult.class, FirstService.class,
                        SecondService.class, FirstServiceClientFactory.class, SecondServiceClientFactory.class)
                .addAsWebInfResource(new File("src/test/resources/web.xml"), "web.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

        return archive;
    }

    @Deployment(name = FIRST_SERVICE_DEPLOYMENT_NAME, managed = false, testable = false)
    @TargetsContainer(FIRST_SERVICE_CONTAINER_NAME)
    public static Archive<?> getFirstServiceDeployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, FIRST_SERVICE_DEPLOYMENT_NAME + ".war")
                .addClasses(FirstService.class, FirstServiceImpl.class, DummyXAResource.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

        return archive;
    }

    @Deployment(name = SECOND_SERVICE_DEPLOYMENT_NAME, managed = false, testable = false)
    @TargetsContainer(SECOND_SERVICE_CONTAINER_NAME)
    public static Archive<?> getSecondServiceDeployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, SECOND_SERVICE_DEPLOYMENT_NAME + ".war")
                .addClasses(SecondService.class, SecondServiceImpl.class, DummyXAResource.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

        return archive;
    }

    @Before
    public void before() {
        startContainers();
        deploy();
        setNodeIdentifier(CLIENT_CONTROLLER, "101");
        setNodeIdentifier(FIRST_SERVICE_CONTROLLER, "102");
        setNodeIdentifier(SECOND_SERVICE_CONTROLLER, "103");

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
