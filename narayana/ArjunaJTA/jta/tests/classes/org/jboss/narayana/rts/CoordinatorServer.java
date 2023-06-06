/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package org.jboss.narayana.rts;

import org.jboss.jbossts.star.util.TxSupport;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@State(Scope.Benchmark)
public class CoordinatorServer {
    @Path("/test")
    public static class Resource {
        @GET
        @Produces("text/plain")
        public String get() {
            return "hello world";
        }
    }

    private static final String APP_PATH = TxSupport.TX_PATH; // "/base"

    @ApplicationPath(APP_PATH)
    public static class MyApp extends Application {
        @Override
        public Set<Class<?>> getClasses() {
            HashSet<Class<?>> classes = new HashSet<>();
            classes.add(Resource.class);

            return classes;
        }
    }
}