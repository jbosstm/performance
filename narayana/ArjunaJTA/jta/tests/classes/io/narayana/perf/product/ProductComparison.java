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

import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.jta.xa.performance.JMHConfigJTA;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.CommandLineOptionException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@State(Scope.Benchmark)
public abstract class ProductComparison {
    public static final int TX_TIMEOUT = 50;
    protected static final String outerClassName = ProductComparison.class.getName();
    private static final int MAX_ERRORS = Integer.getInteger("MAX_ERRORS", 0);

    private AtomicInteger errorCount = new AtomicInteger(0);
    private AtomicInteger completionCount = new AtomicInteger(0);

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
    public void setup() throws Exception {

        worker = getProductWorker();
        worker.init();
        doWork(worker.getProduct());
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

    @Test
    public void test2() throws Exception {
        final int TASK_COUNT = 1000;
        final int THREAD_COUNT = 500;
        final CyclicBarrier gate = new CyclicBarrier(THREAD_COUNT + 1);
        CompletableFuture<Integer>[] futures = new CompletableFuture[TASK_COUNT];
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        System.out.printf("Launching %d futures%n", TASK_COUNT);

        for (int i = 0; i < TASK_COUNT; i++)
            futures[i] = doAsync(gate, i < THREAD_COUNT, i, executor);

        gate.await();

        for (int i = 0; i < TASK_COUNT; i++) {
            if (futures[i].get() != 0)
                System.out.printf("workload %d failed%n", i);
        }

        System.out.printf("Completed %d out of %d workloads%n", completionCount.get(), TASK_COUNT);
    }

    private CompletableFuture<Integer> doAsync(CyclicBarrier gate, boolean wait, int i, ExecutorService executor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (wait)
                    gate.await();

                doWork(worker.getProduct());
                completionCount.incrementAndGet();

                return 0;
            } catch (Exception e) {
                e.printStackTrace();
                return 1;
            }
        }, executor);
    }

    protected void doWork(ProductInterface product) throws Exception {
        try {
            worker.doWork();
        } catch (Exception e) {
            if (errorCount.incrementAndGet() > MAX_ERRORS) {
                e.printStackTrace();

                throw e;
            } else {
                System.err.printf("%s: %s%n", getProductWorker().getName(), e.getMessage());
            }
        }
    }
}
