/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package org.jboss.narayana.rts;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import io.undertow.Undertow;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Application;

@State(Scope.Benchmark)
public class JAXRSServer {
    private UndertowJaxrsServer server;

    public JAXRSServer(String message, int port) {
        System.out.printf("starting undertow (%s)%n", message);
        server = new UndertowJaxrsServer();
        server.start(Undertow.builder().addHttpListener(port, "localhost"));
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

        HttpClient httpClient = HttpClientBuilder.create()
                .setConnectionManager(cm)
                .disableContentCompression().build();
        ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine(httpClient);

        return new ResteasyClientBuilderImpl().httpEngine(engine).build();
    }

    public void stop() {
        server.stop();
    }
}