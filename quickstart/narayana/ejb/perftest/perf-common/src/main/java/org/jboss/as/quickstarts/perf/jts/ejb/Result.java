package org.jboss.as.quickstarts.perf.jts.ejb;

import java.io.Serializable;

/**
 * // TODO: Document this
 *
 * @author mmusgrov
 * @since 4.0
 */
public class Result implements Serializable {
    int nSPort; // jndi port
    long numberOfCalls;
    long errorCount;
    int enlist; // if positive then XA resources are enlisted by each party
    boolean cmt;
    long totalMillis;
    long throughputBMT; // calls per second
    long throughputCMT; // calls per second
    long one; // time in msecs to do one call
    private boolean transactional;
    private long prepareDelay;

    public Result(long numberOfCalls, int enlist, int nSPort, boolean cmt, boolean transactional, long prepareDelay) {
        this.nSPort = nSPort;
        this.numberOfCalls = numberOfCalls;
        this.enlist = enlist;
        this.cmt = cmt;
        this.prepareDelay = prepareDelay;
        this.totalMillis = this.throughputBMT = this.throughputCMT = 0L;
        this.errorCount = 0L;
        this.transactional = transactional;
    }

    public long getNSPort() {
        return nSPort;
    }

    public long getNumberOfCalls() {
        return numberOfCalls;
    }

    public int getEnlist() {
        return enlist;
    }

    public boolean isLocal() {
        return nSPort == 0;
    }

    public long getTotalMillis() {
        return totalMillis;
    }

    public void setTotalMillis(long totalMillis) {
        this.totalMillis = totalMillis;
        this.one = totalMillis > 0 ? totalMillis / numberOfCalls : 0L;
        if (cmt)
            this.throughputCMT = (1000 * numberOfCalls) / totalMillis;
        else
            this.throughputBMT = (1000 * numberOfCalls) / totalMillis;
    }

    public long getThroughputCMT() {
        return throughputCMT;
    }

    public long getThroughputBMT() {
        return throughputBMT;
    }

    public long getOne() {
        return one;
    }

    public boolean isCMT() {
        return cmt;
    }

    public void setCmt(boolean cmt) {
        this.cmt = cmt;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public void incrementErrorCount() {
        this.errorCount += 1;
    }

    public boolean isTransactional() {
        return transactional;
    }

    public void setTransactional(boolean transactional) {
        this.transactional = transactional;
    }

    public long getPrepareDelay() {
        return prepareDelay;
    }
}
