/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */



package com.hp.mwtests.ts.jts.orbspecific.local.performance;

import com.arjuna.ats.internal.jts.OTSImpleManager;
import com.arjuna.ats.internal.jts.orbspecific.TransactionFactoryImple;
import io.narayana.perf.Measurement;
import io.narayana.perf.Worker;
import io.narayana.perf.WorkerLifecycle;
import org.junit.Assert;
import org.junit.Test;
import org.omg.CosTransactions.Control;

public class Performance2 {
    @Test
    public void test() {
        int numberOfCalls = 1000;
        int warmUpCount = 10;
        int numberOfThreads = 1;
        int batchSize = numberOfCalls;

        Measurement measurement = new Measurement.Builder(getClass().getName() + "_test1")
                .maxTestTime(0L).numberOfCalls(numberOfCalls)
                .numberOfThreads(numberOfThreads).batchSize(batchSize)
                .numberOfWarmupCalls(warmUpCount).build().measure(worker, worker);

        Assert.assertNull("Test exception: " + measurement.getException(), measurement.getException());
        Assert.assertEquals(0, measurement.getNumberOfErrors());
        Assert.assertFalse(measurement.getInfo(), measurement.shouldFail());

        System.out.printf("%s%n", measurement.getInfo());
        System.out.println("Average time for empty transaction = " + measurement.getTotalMillis() / (float) numberOfCalls);
        System.out.printf("Transactions per second = %f%n", measurement.getThroughput());
    }

    Worker<Void> worker = new Worker<Void>() {
        WorkerLifecycle<Void> lifecycle = new PerformanceWorkerLifecycle<>();
        TransactionFactoryImple factory = null;

        @Override
        public void init() {
            lifecycle.init();
            factory = OTSImpleManager.factory();
        }

        @Override
        public void fini() {
            lifecycle.fini();
        }

        @Override
        public Void doWork(Void context, int batchSize, Measurement<Void> measurement) {
            for (int i = 0; i < batchSize; i++) {
                try {
                    Control control = factory.create(1);

                    control.get_terminator().commit(true);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }

            return context;
        }

        @Override
        public void finishWork(Measurement<Void> measurement) {
        }
    };
}