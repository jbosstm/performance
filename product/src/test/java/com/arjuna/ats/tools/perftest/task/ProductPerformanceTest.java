/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and/or its affiliates,
 * and individual contributors as indicated by the @author tags.
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
 *
 * (C) 2011,
 * @author JBoss, by Red Hat.
 */
package com.arjuna.ats.tools.perftest.task;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ProductPerformanceTest
{
    private static String[] configKeys = {
            "iterations",
            "warmUpIterations",
            "threads",
            "objectStoreDir",
            "objectStoreType",
            "jts",
            "products",
            "stats",
    };
    private static int BATCH_SIZE = 100;

    private String products[];
    private Map<WorkerTask, TaskResult> tasks;
    private int iterations = 200000;
    private int warmUpIterations = 0;
    private int threads = 100; // 1000 gets an OOM error
    private String productOpt = "com.arjuna.ats.tools.perftest.task.RHWorkerTask";
    private Properties options = new Properties();
    private Calendar calendar = Calendar.getInstance();
    private String skipTestReason = null;
    private PrintWriter output;
    private String hostName;

    private void hackEAPVersion() {
        String profile = System.getProperty("profile");

        if (!profile.startsWith("EAP"))
            return;

        for (int i = 0; i < products.length; i++) {
            if (products[i].endsWith("RHWorkerTask")) {
                if (profile.equals("EAP6")) {
                    products[i] = "com.arjuna.ats.tools.perftest.task.NarayanaWorkerTask";
                } else if (profile.equals("EAP6-JDKORB")) {
                    products[i] = "com.arjuna.ats.tools.perftest.task.NarayanaJdkOrbWorkerTask";
                } else if (profile.equals("EAP5")) {
                    products[i] = "com.arjuna.ats.tools.perftest.task.JBossTSWorkerTask";
                    if (options.getProperty("objectStoreType", "default").endsWith("HornetqObjectStoreAdaptor"))
                        skipTestReason = "Test run ignored - HornetqObjectStoreAdaptor is not compatible with EAP5";
                } else  {
                    System.out.printf("No suitable TS class for profile %s%n", profile);
                }
            }
        }
    }

    private int initTxnCount(int value, boolean verbose) {
        if (value <= 0) {
            if (verbose)
                System.err.printf("Using a sane value for the number of iterations. Changing from %d to %d%n",
                    iterations, BATCH_SIZE);
            value = BATCH_SIZE;
        }

        int txnCount = Math.round(iterations/BATCH_SIZE) * BATCH_SIZE;

        if (txnCount != iterations) {
            if (verbose)
                System.err.printf("Number of iterations must be a multiple of %d - changing it from %d to %d%n",
                    BATCH_SIZE, iterations, txnCount + BATCH_SIZE);
            value = txnCount + BATCH_SIZE;
        }

        return value;
    }

    @Before
    public void setUp() throws Exception {
        String configResource = "test1.properties";
        InputStream bis = Thread.currentThread().getContextClassLoader().getResourceAsStream(configResource);

        skipTestReason = null;

        options.load(bis);

        // allow options set via system properties to override ones set in the config file
        for (String key : configKeys) {
            String val = System.getProperty(key);

            if (val != null)
                options.setProperty(key, val);
        }

        products = options.getProperty("products", productOpt).split(",");
        iterations = Integer.valueOf(options.getProperty("iterations", String.valueOf(iterations)));
        iterations = initTxnCount(iterations, true);
        warmUpIterations = Integer.valueOf(options.getProperty("warmUpIterations", String.valueOf(warmUpIterations)));
        if (warmUpIterations > 0)
            warmUpIterations = initTxnCount(warmUpIterations, true);
        threads = Integer.valueOf(options.getProperty("threads", String.valueOf(threads)));
        tasks = new HashMap<WorkerTask, TaskResult>();

        if (threads < 1)
            threads = 1;

        System.out.printf("iterations=%d threads=%d%n", iterations, threads);

        hackEAPVersion();

        File file = new File(options.getProperty("resultsFile", "target/results.txt"));
        boolean exists = file.exists();
        FileWriter writer = new FileWriter(file, true);

        output = new PrintWriter(writer);

        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostName = "Unknown";
        }

        if (!exists)
            output.printf("%12s %12s %24s %10s %10s %10s %7s %6s %5s %26s %5s %8s %8s %8s%n",
                    "Hostname", "Time", "Product", "Throughput", "Iterations", "WarmUp", "Threads", "Aborts",  "JTS",
                    "Store", "AIO", "HQBuffSz", "SyncDel", "SyncRate");
    }

    @After
    public void tearDown() throws Exception {
        output.close();
    }

    @Test
    public void testProduct() throws Exception {
        TaskResult results[] = new TaskResult[products.length];

        if (skipTestReason != null) {
            String res = String.format("%12tT %s%n", calendar, skipTestReason);
            System.out.print(res);
            output.print(res);
        } else {
            for (int i = 0; i < products.length; i++) {
                if (warmUpIterations > 0)
                    testLoop(products[i], warmUpIterations, 1);
                results[i] = testLoop(products[i], iterations, threads);
            }

            printResults();
        }
    }

    private void printResults() {
        boolean jts = (options.getProperty("jts", "false")).equals("true");
        String store = options.getProperty("objectStoreType", "default");
        int i = store.lastIndexOf('.');

        boolean aio = (System.getProperty("aio", "false")).equals("true");
        int bufferFlushesPerSecond = Integer.getInteger("com.arjuna.ats.arjuna.hornetqjournal.bufferFlushesPerSecond", -1);
        int bufferSize = Integer.getInteger("com.arjuna.ats.arjuna.hornetqjournal.bufferSize", -1);
        String syncDeletes = System.getProperty("com.arjuna.ats.arjuna.hornetqjournal.syncDeletes", "default");

        if (i != -1)
            store = store.substring(i + 1);

        for (Map.Entry<WorkerTask, TaskResult> entry: tasks.entrySet()) {
            WorkerTask task = entry.getKey();
            TaskResult result = entry.getValue();
            String res = String.format("%12s %12tT %24s %10d %10d %10d %7d %6d %5s %26s %5s %8d %8s %8d%n",
                    hostName, calendar, task.getName(),
                    (int) result.getThroughput(), result.iterations, warmUpIterations, result.threads, task.getNumberOfFailures(),
                    jts, store, aio, bufferSize, syncDeletes, bufferFlushesPerSecond);
            output.print(res);
            System.out.print(res);
            task.reportErrors(output);
        }
    }

    private WorkerTask newWorker(
            String className, CyclicBarrier cyclicBarrier, AtomicInteger count, int batch_size) throws Exception {

        Class classDef = Class.forName(className);
        Constructor constructor = classDef.getDeclaredConstructor(
                new Class[]{CyclicBarrier.class, AtomicInteger.class, int.class});
        Object[] args = {cyclicBarrier, count, batch_size};

        return (WorkerTask) constructor.newInstance(args);
    }

    private TaskResult testLoop(String workerClassName, int iterations, int threads) throws Exception {
        AtomicInteger count = new AtomicInteger(iterations/BATCH_SIZE);
        final int nThreads = threads;
        CyclicBarrier cyclicBarrier = new CyclicBarrier(nThreads +1); // workers + self
        ExecutorService executorService = Executors.newCachedThreadPool();
        WorkerTask task = newWorker(workerClassName, cyclicBarrier, count, BATCH_SIZE);

        task.init(options);

        for(int i = 0; i < nThreads; i++)
            executorService.execute(task);

        System.out.print(new Date() + " " + workerClassName);
        long start = System.nanoTime();
        cyclicBarrier.await(); // wait for each thread to arrive at the barrier
        cyclicBarrier.await(); // wait for each thread to finish

        long end = System.nanoTime();
        long duration_ms = (end - start) / 1000000L;

        executorService.shutdown();
        task.fini();

        tasks.put(task, new TaskResult(workerClassName, iterations, threads, duration_ms));

        return tasks.get(task);
    }

    static class TaskResult {
        String product;
        long duration_ms;
        int iterations;
        int threads;

        TaskResult(String product, int iterations, int threads, long duration_ms) {
            this.product = product;
            this.iterations = iterations;
            this.threads = threads;
            this.duration_ms =  duration_ms;
        }

        public double getThroughput() {
            return (1000.0/((1.0*duration_ms)/iterations));
        }

        public StringBuilder toString(StringBuilder sb) {
            sb.append(product);
            sb.append("\n  total time (ms): ").append(duration_ms);
            sb.append("\naverage time (ms): ").append((1.0*duration_ms)/iterations);
            sb.append("\ntx / second: ").append(getThroughput());
            sb.append('\n').append(threads).append(" threads").append('\n');

            return sb;
        }

        public String toString() {
            return toString(new StringBuilder()).toString();
        }
    }
}
