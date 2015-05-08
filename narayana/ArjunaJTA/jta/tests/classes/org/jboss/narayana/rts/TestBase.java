/*
 * Copyright 2015, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.narayana.rts;

import org.jboss.jbossts.star.service.TMApplication;

import javax.ws.rs.client.Client;
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
        txnServer = new JAXRSServer(TXN_PORT);
        txnServer.addDeployment(new TMApplication(), "/");
        txnClient = txnServer.createClient();

        if (TWO_SERVERS)
            svcServer = new JAXRSServer(SVC_PORT);
        else
            svcServer = txnServer;

        svcServer.addDeployment(new TransactionAwareResource.ServiceApp(), "eg");
        svcClient = svcServer.createClient();
    }

    public static void tearDown() throws IOException {
        txnClient.close();
        svcClient.close();
        txnServer.stop();

        if (TWO_SERVERS)
            svcServer.stop();
    }
}
