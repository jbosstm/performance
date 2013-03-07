package narayana.performance.web;


import narayana.performance.beans.HelloWorld;
import narayana.performance.util.Lookup;
import narayana.performance.util.Result;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class PerfTest extends HttpServlet {
    private static int BATCH_SIZE = 100;
    private static final int POOL_SIZE = 50;
    private static final String jndiName = "HelloWorldJNDIName";
    private HelloWorld localBean;
    private ExecutorService executor;
    private String objStoreType;

    @Override
    public void init() throws ServletException {
        super.init();

        executor = Executors.newFixedThreadPool(POOL_SIZE);
        objStoreType = System.getProperty("com.arjuna.ats.arjuna.objectstore.objectStoreType", "Unknown");

        try {
            Context ctx = Lookup.getContext("localhost:1099");
            localBean = (HelloWorld) ctx.lookup(jndiName);
            if (localBean == null)
                throw new ServletException("error looking up HelloWorld");
        } catch (NamingException e) {
            throw new ServletException(e);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        executor.shutdownNow();
    }

    protected Result doWork(final Result opts) throws Exception {
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
        cyclicBarrier.await(); // wait for each thread to arrive at the barrier
        cyclicBarrier.await(); // wait for each thread to finish
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

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Result opts = Result.toResult(request.getParameterMap());

        opts.setStoreType(objStoreType);
        opts.setCmt(false);
        response.setContentType("text/html;charset=UTF-8");

        PrintWriter out = response.getWriter();

        if (opts.isVerbose())
            out.print(opts.getHeader());

        try {
            opts = doWork(opts);
        } catch (Exception e) {
            throw new ServletException(e);
        }

        out.print(opts.toString());

        if (opts.isVerbose() && opts.isUseHtml())
            out.println("</body></html>");

    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}
