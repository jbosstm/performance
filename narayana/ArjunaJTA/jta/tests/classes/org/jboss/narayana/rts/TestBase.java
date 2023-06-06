/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package org.jboss.narayana.rts;

import org.jboss.jbossts.star.service.TMApplication;

import jakarta.ws.rs.client.Client;
import java.io.IOException;

public class TestBase {
    static boolean TWO_SERVERS = false;
    static final int TXN_PORT = 8090;
    static final int SVC_PORT = TWO_SERVERS ? 8092 : 8090;

    static final String TXN_URL = String.format("http://%s:%d%s", "localhost", TXN_PORT, "/tx/transaction-manager");
    static final String SVC_URL = String.format("http://%s:%d%s", "localhost", SVC_PORT, "/eg/service");

    static JAXRSServer txnServer;
    static JAXRSServer svcServer;
    static Client txnClient;
    static Client svcClient;

    public static void setup() throws Exception {
        txnServer = new JAXRSServer("service1", TXN_PORT);
        txnServer.addDeployment(new TMApplication(), "/");
        txnClient = txnServer.createClient();

        if (TWO_SERVERS)
            svcServer = new JAXRSServer("service2", SVC_PORT);
        else
            svcServer = txnServer;

        svcServer.addDeployment(new TransactionAwareResource.ServiceApp(), "eg");
        svcClient = svcServer.createClient();
        Util.emptyObjectStore();
    }

    public static void tearDown() throws IOException {
        txnClient.close();
        svcClient.close();
        txnServer.stop();

        if (TWO_SERVERS)
            svcServer.stop();
    }
}