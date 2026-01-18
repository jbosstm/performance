/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */
package com.hp.mwtests.ts.arjuna.performance;

import com.arjuna.ats.arjuna.AtomicAction;
import com.arjuna.ats.arjuna.common.CoordinatorEnvironmentBean;
import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.arjuna.coordinator.TwoPhaseCommitThreadPool;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hp.mwtests.ts.arjuna.performance.VTPerformanceTest.THREADS;

/*
 * benchmark 2PC comparing synchronous vs asynchronous threads (using Executors.newFixedThreadPool and
 * Executors.newVirtualThreadPerTaskExecutor)
 */
@OutputTimeUnit(TimeUnit.SECONDS)
@Threads(THREADS)
@Fork(VTPerformanceTest.FORKS) // run the benchmark in two processes/JVMs
@Warmup(iterations = VTPerformanceTest.ITERATIONS, time = VTPerformanceTest.TIME_PER_ITER, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = VTPerformanceTest.ITERATIONS, time = VTPerformanceTest.TIME_PER_ITER, timeUnit = TimeUnit.SECONDS)
public class VTPerformanceTest {
    static final int THREADS = 10;//_000; //Threads.MAX;
    static final int FORKS = 2;
    static final int ITERATIONS = 5;
    static final int TIME_PER_ITER = 2;

    private static final String MS_DELAY = "100"; // 100 ms (simulate a network delay)
    private static final String RESTART_EXECUTOR_METHOD_NAME = "restartExecutor";
    private static final String SHUTDOWN_EXECUTOR_METHOD_NAME = "shutdownExecutorNow";
    private static MethodHandle restartExecutorMH; // method handle to restart the executor
    private static MethodHandle shutdownExecutorMH; // method handle to shut down the executor
    private static boolean isVirtualThreadPerTaskExecutor;

    @State(Scope.Benchmark)
    public static class VTBenchmarkState {
        @Param({"0", MS_DELAY}) // run the benchmark twice with and without simulating a network delay
        int networkDelay;

        @Setup(Level.Trial)
        public void doSetup() {
            beforeClass(this.getClass().getName(), true, true);
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            List<Runnable> pending = shutdownExecutorNow();
            if (!pending.isEmpty()) {
                System.out.printf("%s tearDown executor left %d tasks:%n", getClass().getName(), pending.size());
                pending.forEach(System.out::println);
            }
        }
    }

    @State(Scope.Benchmark)
    public static class STBenchmarkState {
        @Param({"0", MS_DELAY})
        int networkDelay;

        @Setup(Level.Trial)
        public void doSetup() {
            beforeClass(this.getClass().getName(), true, false);
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            List<Runnable> pending = shutdownExecutorNow();
            if (!pending.isEmpty()) {
                System.out.printf("%s tearDown executor left %d tasks:%n", getClass().getName(), pending.size());
                pending.forEach(System.out::println);
            }
        }
    }

    @State(Scope.Benchmark)
    public static class BasicBenchmarkState {
        @Param({"0", MS_DELAY})
        int networkDelay;

        @Setup(Level.Trial)
        public void doSetup() {
            beforeClass(this.getClass().getName(), false, false);
        }
    }

    @Benchmark
    public void virtualThreadsBenchmark(VTBenchmarkState state) {
        twoPhase(state.networkDelay);
    }

    @Benchmark
    public void standardThreadsBenchmark(STBenchmarkState state) {
        twoPhase(state.networkDelay);
    }

    @Benchmark
    public void baselineBenchmark(BasicBenchmarkState state) {
        twoPhase(state.networkDelay);
    }

    private void twoPhase(int msDelay) {
        AtomicAction A = new AtomicAction();

        A.begin();
        A.add(new BasicRecord2(msDelay));
        A.add(new BasicRecord2(msDelay));
        A.commit();
    }

    public static void beforeClass(String msg, boolean async, boolean enableVT) {
        CoordinatorEnvironmentBean configBean = arjPropertyManager.getCoordinatorEnvironmentBean();

        configBean.setAsyncPrepare(async);
        configBean.setAsyncCommit(async);
        configBean.setAsyncRollback(async);

        configBean.setAsyncBeforeSynchronization(async);
        configBean.setAsyncAfterSynchronization(async);

        // TwoPhaseCommitThreadPool.restartExecutor executor is required, but it is package private
        // so we need to use method handles (or reflection):

        // create the lookup
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        // lookup the method
        try {
            Method restartMethod = TwoPhaseCommitThreadPool.class.getDeclaredMethod(RESTART_EXECUTOR_METHOD_NAME);
            // it's package private so make it accessible
            restartMethod.setAccessible(true);
            // and get a method handle to it
            restartExecutorMH = lookup.unreflect(restartMethod);

            Method shutdownMethod = TwoPhaseCommitThreadPool.class.getDeclaredMethod(SHUTDOWN_EXECUTOR_METHOD_NAME);
            shutdownMethod.setAccessible(true);
            shutdownExecutorMH = lookup.unreflect(shutdownMethod);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        isVirtualThreadPerTaskExecutor = restartExecutor(enableVT);
        System.out.printf("%s: setup: isVirtualThreadPerTaskExecutor=%b (enableVT=%b)%n",
                msg, isVirtualThreadPerTaskExecutor, enableVT);
    }

    private static List<Runnable> shutdownExecutorNow() {
        try {
            // invoke the shutdownExecutor method on TwoPhaseCommitThreadPool
            return (List<Runnable>) shutdownExecutorMH.invokeExact();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean restartExecutor(boolean useVirtualThreads) {
        try {
            arjPropertyManager.getCoordinatorEnvironmentBean().
                    setUseVirtualThreadsForTwoPhaseCommitThreads(useVirtualThreads);

            // invoke the restartExecutor method on TwoPhaseCommitThreadPool
            isVirtualThreadPerTaskExecutor = (boolean) restartExecutorMH.invokeExact();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        return isVirtualThreadPerTaskExecutor;
    }
}