package narayana.performance.util;

import java.io.Serializable;

/**
 * // TODO: Document this
 *
 * @author mmusgrov
 * @since 4.0
 */
public class Result implements Serializable {
    String productVersion = "xxx";
    String patchedJacorb = "unknown";
    String storeType = "unknown";
    int threadCount = 1;
    String info = "";

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
    private boolean iiop;

    public Result(String info) {
        this.info = info;
    }

    public static StringBuilder getHeader(StringBuilder sb) {
        return sb.append(String.format("%9s %11s %9s %9s %9s %9s %9s %9s %9s %9s",
                "Version", "Throughput", "Calls", "Errors", "Patched", "FileStore", "Threads", "Transaction", "Enlist", "Remote"));
    }

    public String toString() {
        return String.format("%9s %11d %9d %9d %9s %9s %9d %11b %9d %9d",
                productVersion, getThroughputBMT(), getNumberOfCalls(), getErrorCount(), patchedJacorb, storeType,
                threadCount, transactional, enlist, nSPort);
    }

    public Result(long numberOfCalls, int enlist, int nSPort, boolean iiop, boolean cmt, boolean transactional, long prepareDelay) {
        this.nSPort = nSPort;
        this.numberOfCalls = numberOfCalls;
        this.enlist = enlist;
        this.cmt = cmt;
        this.prepareDelay = prepareDelay;
        this.totalMillis = this.throughputBMT = this.throughputCMT = 0L;
        this.errorCount = 0L;
        this.transactional = transactional;
        this.iiop = iiop;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
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

    public boolean isIiop() {
        return iiop;
    }

    public void setTransactional(boolean transactional) {
        this.transactional = transactional;
    }

    public long getPrepareDelay() {
        return prepareDelay;
    }

    public static Result getDefaultOpts() {
        return new Result(100, 1, 1199, true, false, true, 0);
    }

    public static Result validateOpts(Result opts) {
        if (opts == null)
            opts = getDefaultOpts();

        return opts;
    }
}
