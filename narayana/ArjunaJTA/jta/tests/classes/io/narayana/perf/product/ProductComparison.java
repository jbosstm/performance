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
package io.narayana.perf.product;

import org.junit.After;
import org.junit.Before;

import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.runner.BenchmarkException;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.CommandLineOptionException;
import com.arjuna.ats.jta.xa.performance.JMHConfigJTA;

import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;

import java.util.concurrent.atomic.AtomicInteger;


@State(Scope.Benchmark)
abstract public class ProductComparison {
    final protected static String outerClassName = ProductComparison.class.getName();
    final static private int MAX_ERRORS = Integer.getInteger("MAX_ERRORS", 0);

    private AtomicInteger errorCount = new AtomicInteger(0);

    private ProductWorker<Void> worker;

    public static void main(String[] args) throws RunnerException, CommandLineOptionException, CoreEnvironmentBeanException {
        JMHConfigJTA.runJTABenchmark(ProductComparison.class.getSimpleName(), args);
    }

    protected abstract ProductInterface getProductInterface();

    protected ProductWorker<Void> getProductWorker() {
        return new ProductWorker<Void>(getProductInterface());
    }

    @Before
    @Setup
    public void setup() {
        worker = getProductWorker();
        worker.init();
        System.out.printf("benchmarking %s%n", worker.getName());
    }

    @After
    @TearDown
    public void tearDown() {
        worker.fini();
    }

    @Test
    @Benchmark
    public void test() throws Exception {
        doWork(worker.getProduct());
    }

    protected void doWork(ProductInterface product) throws Exception {
        try {
            worker.doWork();
        } catch (Exception e) {
            if (errorCount.incrementAndGet() > MAX_ERRORS) {
                e.printStackTrace();

                throw new BenchmarkException(e);
            } else {
                System.err.printf("%s: %s%n", getProductWorker().getName(), e.getMessage());
            }
        }
    }
}
