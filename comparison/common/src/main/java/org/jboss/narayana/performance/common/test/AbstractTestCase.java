/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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

    protected static final String CLIENT_CONTAINER_NAME = "server1";

    protected static final String FIRST_SERVICE_CONTAINER_NAME = "server2";

    protected static final String SECOND_SERVICE_CONTAINER_NAME = "server3";

    protected static final String CLIENT_DEPLOYMENT_NAME = "client-deployment";

    protected static final String FIRST_SERVICE_DEPLOYMENT_NAME = "first-service-deployment";

    protected static final String SECOND_SERVICE_DEPLOYMENT_NAME = "second-service-deployment";

    protected static final String CLIENT_CONTROLLER = "127.0.0.1:9990";

    protected static final String FIRST_SERVICE_CONTROLLER = "127.0.0.1:10090";

    protected static final String SECOND_SERVICE_CONTROLLER = "127.0.0.1:10190";

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
                System.getProperty("server1.jvm.args").trim()).map();

        final Map<String, String> config2 = new Config().add("javaVmArguments",
                System.getProperty("server2.jvm.args").trim() + " -Djboss.socket.binding.port-offset=100").map();

        final Map<String, String> config3 = new Config().add("javaVmArguments",
                System.getProperty("server3.jvm.args").trim() + " -Djboss.socket.binding.port-offset=200").map();

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

    protected void enableJTS(final String controller) {
        try {
            final CommandContext context = CommandContextFactory.getInstance().newCommandContext();
            context.connectController(controller);
            context.handle("connect " + controller);
            context.handle("/subsystem=jacorb:write-attribute(name=transactions,value=on)");
            context.handle("/subsystem=transactions:write-attribute(name=jts,value=true)");
            context.handle("exit");
        } catch (final CommandLineException e) {
            throw new RuntimeException("Failed to enable JTS on controller: " + controller, e);
        }
    }

    protected void setNodeIdentifier(final String controller, final String nodeIdentifier) {
        try {
            final CommandContext context = CommandContextFactory.getInstance().newCommandContext();
            context.connectController(controller);
            context.handle("connect " + controller);
            context.handle("/subsystem=transactions:write-attribute(name=node-identifier,value=" + nodeIdentifier + ")");
            context.handle("exit");
        } catch (final CommandLineException e) {
            throw new RuntimeException("Failed to set node identifier on controller: " + controller, e);
        }
    }

    protected void enableOnlyWarningLogs(final String controller) {
        try {
            final CommandContext context = CommandContextFactory.getInstance().newCommandContext();
            context.connectController(controller);
            context.handle("connect " + controller);
            context.handle("/subsystem=logging/console-handler=CONSOLE:write-attribute(name=level,value=WARN)");
            context.handle("exit");
        } catch (final CommandLineException e) {
            throw new RuntimeException("Failed to set console log handler's level on controller: " + controller, e);
        }
    }

    protected void enableTraceLogs(final String controller) {
        try {
            final CommandContext context = CommandContextFactory.getInstance().newCommandContext();
            context.connectController(controller);
            context.handle("connect " + controller);
            context.handle("/subsystem=logging/console-handler=CONSOLE:write-attribute(name=level,value=TRACE)");
            context.handle("/subsystem=logging/logger=com.arjuna:change-log-level");
            context.handle("/subsystem=logging/logger=org.jboss.narayana:add(level=TRACE)");
            context.handle("exit");
        } catch (final CommandLineException e) {
            throw new RuntimeException("Failed to set console log handler's level on controller: " + controller, e);
        }
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
