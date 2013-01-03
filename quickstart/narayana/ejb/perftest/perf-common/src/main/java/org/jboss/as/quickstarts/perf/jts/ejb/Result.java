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
    boolean enlist; // if true then an XA resource is enlisted by each party
    boolean local; // if false then calls are to a remote server
    long totalMillis;
    long throughput; // calls per second
    long one; // time in msecs to do one call

    public Result(long numberOfCalls, boolean enlist, int nSPort) {
        this.nSPort = nSPort;
        this.numberOfCalls = numberOfCalls;
        this.enlist = enlist;
        this.local = local;
        this.totalMillis = this.throughput = 0L;
    }

    public long getNSPort() {
        return nSPort;
    }

    public long getNumberOfCalls() {
        return numberOfCalls;
    }

    public boolean isEnlist() {
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
        this.throughput = (1000 * numberOfCalls) / totalMillis;
    }

    public long getThroughput() {
        return throughput;
    }

    public long getOne() {
        return one;
    }
}
