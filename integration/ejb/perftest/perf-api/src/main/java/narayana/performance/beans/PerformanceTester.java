package narayana.performance.beans;

import narayana.performance.util.Result;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


public class PerformanceTester {
    private int BATCH_SIZE;
    private int POOL_SIZE;
    private ExecutorService executor;
    private String objStoreType;

    public PerformanceTester(String maxThreads, String batchSize) {
        POOL_SIZE = maxThreads != null ? Integer.parseInt(maxThreads) : 32; // must be >=  jacorb.poa.thread_pool_max
        BATCH_SIZE = batchSize != null ? Integer.parseInt(batchSize) : 100;

        executor = Executors.newFixedThreadPool(POOL_SIZE);
        objStoreType = System.getProperty("com.arjuna.ats.arjuna.objectstore.objectStoreType", "Unknown");
    }

    public void fini() {
        executor.shutdownNow();
    }

    private Result doWork(final HelloWorld localBean, final Result opts)  {
        int threadCount = opts.getThreadCount();
        int callCount = opts.getNumberOfCalls();

        if (threadCount == 1)
            return localBean.doWork(opts);

        if (threadCount > POOL_SIZE) {
            System.err.println("Updating thread count (request size exceeds thread pool size)");
            threadCount = POOL_SIZE;
            opts.setThreadCount(POOL_SIZE);
        }

        if (callCount < BATCH_SIZE) {
            System.err.println("Updating call count (request size less than batch size)");
            callCount = BATCH_SIZE;
            opts.setNumberOfCalls(callCount);
        }

        int batchCount =  callCount/BATCH_SIZE;

        if (batchCount < threadCount) {
            System.err.println("Reducing thread count (request number greater than the number of batches)");
            threadCount = batchCount;
            opts.setThreadCount(threadCount);
        }

        final AtomicInteger count = new AtomicInteger(callCount/BATCH_SIZE);

        Collection<Future<Result>> tasks = new ArrayList<Future<Result>>();
        final CyclicBarrier cyclicBarrier = new CyclicBarrier(threadCount + 1); // workers + self

        for (int i = 0; i < opts.getThreadCount(); i++)
            tasks.add(executor.submit(new Callable<Result>() {
                public Result call() throws Exception {
                    Result res = new Result(opts);
                    int errorCount = 0;
                    long start = System.nanoTime();

                    cyclicBarrier.await();

                    while(count.decrementAndGet() >= 0) {
                        res.setNumberOfCalls(BATCH_SIZE);
                        localBean.doWork(res);
                        errorCount += res.getErrorCount();
                    }

                    cyclicBarrier.await();

                    res.setTotalMillis((System.nanoTime() - start) / 1000000L);
                    res.setErrorCount(errorCount);

                    return res;
                };
            }));

        long start = System.nanoTime();

        try {
            cyclicBarrier.await(); // wait for each thread to arrive at the barrier
            cyclicBarrier.await(); // wait for each thread to finish
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (BrokenBarrierException e) {
            throw new RuntimeException(e);
        }

        long end = System.nanoTime();

        opts.setTotalMillis(0L);
        opts.setErrorCount(0);

        for (Future<Result> t : tasks) {
            try {
                Result outcome = t.get();

                opts.setTotalMillis(opts.getTotalMillis() + outcome.getTotalMillis());
                opts.setErrorCount(opts.getErrorCount() + outcome.getErrorCount());
            } catch (Exception e) {
                opts.setErrorCount(opts.getErrorCount() + BATCH_SIZE);
            }
        }

        opts.setTotalMillis((end - start) / 1000000L);
        return opts;
    }

    protected Result measureThroughput(HelloWorld bean, Result opts) {
        return doWork(bean, opts);
    }

    public Result measureThroughput(PrintWriter out, HelloWorld bean, Result opts) {
        opts.setStoreType(objStoreType);
        opts.setCmt(false);

        if (opts.isVerbose())
            out.print(opts.getHeader());

        Result result = measureThroughput(bean, opts);

        out.print(opts.toString());

        if (opts.isVerbose() && opts.isUseHtml())
            out.println("</body></html>");

        return result;
    }
}

