package org.jboss.narayana.performance.rts;

import java.io.File;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.narayana.performance.common.test.AbstractTestCase;
import org.jboss.narayana.performance.common.test.TestResult;
import org.jboss.narayana.performance.common.xa.DummyXAResource;
import org.jboss.narayana.performance.rts.client.TestExecutor;
import org.jboss.narayana.performance.rts.client.TestWorkerImpl;
import org.jboss.narayana.performance.rts.service.AbstractService;
import org.jboss.narayana.performance.rts.service.first.FirstService;
import org.jboss.narayana.performance.rts.service.second.SecondService;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
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

    private static final String DEPENDENCIES = "Dependencies: org.jboss.jts,, org.jboss.narayana.rts\n";

    private static final String BASE_URL = "http://127.0.0.1";

    private static final String EXECUTOR_URL = BASE_URL + ":8180/" + CLIENT_DEPLOYMENT_NAME + "/"
            + TestExecutor.RESOURCE_PATH;

    private static final String FIRST_SERVICE_URL = BASE_URL + ":8180/" + FIRST_SERVICE_DEPLOYMENT_NAME;

    private static final String SECOND_SERVICE_URL = BASE_URL + ":8280/" + SECOND_SERVICE_DEPLOYMENT_NAME;

    private static final String COORDINATOR_URL = BASE_URL + ":8180/rest-at-coordinator/tx/transaction-manager";

    @Deployment(name = CLIENT_DEPLOYMENT_NAME, managed = false, testable = false)
    @TargetsContainer(CLIENT_CONTAINER_NAME)
    public static Archive<?> getClientDeployment() {
        final WebArchive archive = ShrinkWrap
                .create(WebArchive.class, CLIENT_DEPLOYMENT_NAME + ".war")
                .addPackage("io.narayana.perf")
                .addClasses(TestExecutor.class, TestWorkerImpl.class, TestResult.class)
                .addAsWebInfResource(new File("src/test/resources/web.xml"), "web.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(new StringAsset(DEPENDENCIES), "MANIFEST.MF");

        return archive;
    }

    @Deployment(name = FIRST_SERVICE_DEPLOYMENT_NAME, managed = false, testable = false)
    @TargetsContainer(FIRST_SERVICE_CONTAINER_NAME)
    public static Archive<?> getFirstServiceDeployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, FIRST_SERVICE_DEPLOYMENT_NAME + ".war")
                .addClasses(AbstractService.class, FirstService.class, DummyXAResource.class)
                .addAsWebInfResource(new File("src/test/resources/web.xml"), "web.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(new StringAsset(DEPENDENCIES), "MANIFEST.MF");

        return archive;
    }

    @Deployment(name = SECOND_SERVICE_DEPLOYMENT_NAME, managed = false, testable = false)
    @TargetsContainer(SECOND_SERVICE_CONTAINER_NAME)
    public static Archive<?> getSecondServiceDeployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, SECOND_SERVICE_DEPLOYMENT_NAME + ".war")
                .addClasses(AbstractService.class, SecondService.class, DummyXAResource.class)
                .addAsWebInfResource(new File("src/test/resources/web.xml"), "web.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(new StringAsset(DEPENDENCIES), "MANIFEST.MF");

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
                .queryParam("numberOfCalls", numberOfCalls).queryParam("batchSize", batchSize)
                .queryParam("coordinatorUrl", COORDINATOR_URL).queryParam("firstServiceUrl", FIRST_SERVICE_URL)
                .queryParam("secondServiceUrl", SECOND_SERVICE_URL).request().get(TestResult.class);

        Assert.assertNotNull(result);
        exportTestResults(result);
    }

}
