package org.jboss.narayana.performance.common.test;

import io.narayana.perf.Result;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@XmlRootElement(name = "result")
public class TestResult {

    private int throughput;

    private long averageTime;

    private long totalTime;

    private int numberOfErrors;

    private int numberOfCalls;

    private int numberOfThreads;

    public TestResult() {

    }

    public TestResult(final int throughput, final long averageTime, final long totalTime, final int numberOfErrors,
            final int numberOfCalls, final int numberOfThreads) {

        this.throughput = throughput;
        this.averageTime = averageTime;
        this.totalTime = totalTime;
        this.numberOfErrors = numberOfErrors;
        this.numberOfCalls = numberOfCalls;
        this.numberOfThreads = numberOfThreads;
    }

    public TestResult(final Result<String> result) {
        throughput = result.getThroughput();
        averageTime = result.getOne();
        totalTime = result.getTotalMillis();
        numberOfErrors = result.getErrorCount();
        numberOfCalls = result.getNumberOfCalls();
        numberOfThreads = result.getThreadCount();
    }

    @XmlElement
    public int getThroughput() {
        return throughput;
    }

    public void setThroughput(final int throughput) {
        this.throughput = throughput;
    }

    @XmlElement
    public long getAverageTime() {
        return averageTime;
    }

    public void setAverageTime(final long averageTime) {
        this.averageTime = averageTime;
    }

    @XmlElement
    public long getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(final long totalTime) {
        this.totalTime = totalTime;
    }

    @XmlElement
    public int getNumberOfErrors() {
        return numberOfErrors;
    }

    public void setNumberOfErrors(final int numberOfErrors) {
        this.numberOfErrors = numberOfErrors;
    }

    @XmlElement
    public int getNumberOfCalls() {
        return numberOfCalls;
    }

    public void setNumberOfCalls(final int numberOfCalls) {
        this.numberOfCalls = numberOfCalls;
    }

    @XmlElement
    public int getNumberOfThreads() {
        return numberOfThreads;
    }

    public void setNumberOfThreads(final int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
    }

    @Override
    public String toString() {
        return "TestResult [throughput=" + throughput + ", averageTime=" + averageTime + ", totalTime=" + totalTime
                + ", numberOfErrors=" + numberOfErrors + ", numberOfCalls=" + numberOfCalls + ", numberOfThreads="
                + numberOfThreads + "]";
    }

}
