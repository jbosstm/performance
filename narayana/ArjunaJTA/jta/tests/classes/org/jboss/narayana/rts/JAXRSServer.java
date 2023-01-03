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

import io.undertow.Undertow;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;

import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.jboss.jbossts.star.service.TMApplication;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Application;

@State(Scope.Benchmark)
public class JAXRSServer {
    private UndertowJaxrsServer server;

    public JAXRSServer(int port) {
        System.out.printf("starting undertow%n");
        server = new UndertowJaxrsServer();
        server.start(Undertow.builder().addHttpListener(port, "localhost"));

        server.deploy(new TMApplication(), "/");
        server.deploy(new TransactionAwareResource.ServiceApp(), "eg");
    }


    public void addDeployment(Application application, String contextRoot) {
        server.deploy(application, contextRoot);
    }

    private void setCMConfig(PoolingHttpClientConnectionManager cm) {
        cm.setMaxTotal(100);
        cm.setDefaultMaxPerRoute(10);
        cm.setMaxPerRoute(new HttpRoute(new HttpHost("localhost", 80)), 20);
    }

    public Client createClient() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();

        setCMConfig(cm);

        HttpClient httpClient = HttpClients.createMinimal(cm);

        ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine(httpClient);

        return new ResteasyClientBuilderImpl().httpEngine(engine).build();
    }

    public void stop() {
        server.stop();
    }
}
