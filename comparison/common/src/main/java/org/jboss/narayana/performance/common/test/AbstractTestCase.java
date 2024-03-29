/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package org.jboss.narayana.performance.common.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.jboss.arquillian.container.test.api.Config;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContextFactory;
import org.jboss.as.cli.CommandLineException;

/**
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 *
 */
public abstract class AbstractTestCase {
    protected static final int HTTP_REMOTING_PORT = 9990;
    protected static final int HTTP_PORT = 8080;

    protected static final String CLIENT_CONTAINER_NAME = "server1";
    protected static final int CLIENT_CONTAINER_OFFSET = 100;

    protected static final String FIRST_SERVICE_CONTAINER_NAME = "server2";
    protected static final int FIRST_SERVICE_CONTAINER_OFFSET = 200;

    protected static final String SECOND_SERVICE_CONTAINER_NAME = "server3";
    protected static final int SECOND_SERVICE_CONTAINER_OFFSET = 300;

    protected static final String CLIENT_DEPLOYMENT_NAME = "client-deployment";

    protected static final String FIRST_SERVICE_DEPLOYMENT_NAME = "first-service-deployment";

    protected static final String SECOND_SERVICE_DEPLOYMENT_NAME = "second-service-deployment";


    protected static final String CLIENT_CONTROLLER = "http-remoting://localhost:" + (HTTP_REMOTING_PORT + CLIENT_CONTAINER_OFFSET);

    protected static final String FIRST_SERVICE_CONTROLLER = "http-remoting://localhost:" + (HTTP_REMOTING_PORT + FIRST_SERVICE_CONTAINER_OFFSET);

    protected static final String SECOND_SERVICE_CONTROLLER = "http-remoting://localhost:" + (HTTP_REMOTING_PORT + SECOND_SERVICE_CONTAINER_OFFSET);

    protected final String numberOfThreads;

    protected final String numberOfCalls;

    protected final String batchSize;

    @ArquillianResource
    protected ContainerController controller;

    @ArquillianResource
    protected Deployer deployer;

    public AbstractTestCase() {
        numberOfThreads = System.getProperty("numberOfThreads", "1");
        numberOfCalls = System.getProperty("numberOfCalls", "50");
        batchSize = System.getProperty("batchSize", "1");
    }

    protected void startContainers() {
        final Map<String, String> config1 = new Config().add("javaVmArguments",
                System.getProperty("server1.jvm.args").trim() + " -Djboss.socket.binding.port-offset="
                        + CLIENT_CONTAINER_OFFSET + " -Djboss.node.name=" + CLIENT_CONTAINER_NAME).map();

        final Map<String, String> config2 = new Config().add("javaVmArguments",
                System.getProperty("server2.jvm.args").trim() + " -Djboss.socket.binding.port-offset="
                        + FIRST_SERVICE_CONTAINER_OFFSET + " -Djboss.node.name=" + FIRST_SERVICE_CONTAINER_NAME).map();

        final Map<String, String> config3 = new Config().add("javaVmArguments",
                System.getProperty("server3.jvm.args").trim() + " -Djboss.socket.binding.port-offset="
                        + SECOND_SERVICE_CONTAINER_OFFSET  + " -Djboss.node.name=" + SECOND_SERVICE_CONTAINER_NAME).map();

        controller.start(CLIENT_CONTAINER_NAME, config1);
        controller.start(FIRST_SERVICE_CONTAINER_NAME, config2);
        controller.start(SECOND_SERVICE_CONTAINER_NAME, config3);
    }

    protected void stopContainers() {
        try {
            controller.stop(CLIENT_CONTAINER_NAME);
        } finally {
            try {
                controller.stop(FIRST_SERVICE_CONTAINER_NAME);
            } finally {
                controller.stop(SECOND_SERVICE_CONTAINER_NAME);
            }
        }
    }

    protected void restartContainers() {
        stopContainers();
        startContainers();
    }

    protected void deploy() {
        deployer.deploy(CLIENT_DEPLOYMENT_NAME);
        deployer.deploy(FIRST_SERVICE_DEPLOYMENT_NAME);
        deployer.deploy(SECOND_SERVICE_DEPLOYMENT_NAME);
    }

    protected void undeploy() {
        deployer.undeploy(CLIENT_DEPLOYMENT_NAME);
        deployer.undeploy(FIRST_SERVICE_DEPLOYMENT_NAME);
        deployer.undeploy(SECOND_SERVICE_DEPLOYMENT_NAME);
    }

    protected void executeCommands(String controllerUri, String errMsg, String ... cmds) {
        try {
            final CommandContext context = CommandContextFactory.getInstance().newCommandContext();

            context.connectController(controllerUri);

            for (String cmd : cmds)
                context.handle(cmd);
        } catch (final CommandLineException e) {
            throw new RuntimeException(e.getMessage() + ": " + errMsg + " on controller: " + controllerUri, e);
        }
    }

    protected void enableJTS(final String controller) {
        executeCommands(controller,
                "Failed to enable JTS",
                "/subsystem=iiop-openjdk:write-attribute(name=transactions,value=full)",
                "/subsystem=transactions:write-attribute(name=jts,value=true)",
                "exit");
    }

    protected void setNodeIdentifier( String controller, final String nodeIdentifier) {
        executeCommands(controller,
                "Failed to set node identifier",
                "/subsystem=transactions:write-attribute(name=node-identifier,value=" + nodeIdentifier + ")",
                "exit");
    }

    protected void enableOnlyWarningLogs(final String controller) {
        executeCommands(controller,
                "Failed to set console log handler's level",
                "/subsystem=logging/console-handler=CONSOLE:write-attribute(name=level,value=WARN)",
                "exit");
    }

    protected void enableTraceLogs(final String controller) {
        executeCommands(controller,
                "Failed to set console log handler's level",
                "/subsystem=logging/console-handler=CONSOLE:write-attribute(name=level,value=TRACE)",
                "/subsystem=logging/logger=com.arjuna:change-log-level",
                "/subsystem=logging/logger=org.jboss.narayana:add(level=TRACE)",
                "exit");
    }

    protected void exportTestResults(final TestResult testResult) {
        final File file = new File("target/performance_test_result.txt");
        BufferedWriter bufferedWriter = null;

        try {
            final FileWriter fileWriter = new FileWriter(file.getAbsoluteFile());
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(testResult.toString());
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            if (bufferedWriter != null) {
                try {
                    bufferedWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}