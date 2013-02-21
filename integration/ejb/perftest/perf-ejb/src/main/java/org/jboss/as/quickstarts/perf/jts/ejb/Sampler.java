package org.jboss.as.quickstarts.perf.jts.ejb;

import org.javasimon.SimonManager;
import org.javasimon.Stopwatch;

/**
 * Prints sampled values from the stopwatch every 10 seconds + resets the Simon.
 */
class Sampler extends Thread {
    private Stopwatch[] stopwatches;
    private boolean report = false;
    long period = 10000L;

    Sampler(Stopwatch[] stopwatches, long period) {
        this.stopwatches = stopwatches;
        this.period = period;
    }

    public void setReport(boolean report) {
        this.report = report;
    }

    public void report() {
        for (Stopwatch stopwatch : stopwatches)
            System.out.printf("%s: tot: %d mean: %f%n\n", stopwatch.getName(), stopwatch.getTotal(), stopwatch.getMean());

        System.out.println();
    }

    public void report2() {
        for (Stopwatch stopwatch : stopwatches) {
            System.out.println("\nstopwatch = " + stopwatch);
            System.out.println("Stopwatch sample: " + stopwatch.sample());
        }
    }

    public void run() {
        while (true) {
            if (report) {
                report();
            }
            // uncomment this if you want reset - of course comment the line above
            //                              System.out.println("Stopwatch sample: " + stopwatch.sampleAndReset());
            try {
                Thread.sleep(period);
            } catch (InterruptedException e) {
            }
        }
    }
}