package narayana.performance.util;

import java.io.Serializable;
import java.util.Map;

/**
 * // TODO: Document this
 *
 * @author mmusgrov
 * @since 4.0
 */
public class Result implements Serializable {
    String productVersion = "Unknown";
    String patchedJacorb = "Unknown";
    String storeType = "unknown";
    int threadCount = 1;
    String info = "";
    String namingProvider;
    boolean local;
    int nSPort; // jndi port
    int numberOfCalls;
    int errorCount;
    int enlist; // if positive then XA resources are enlisted by each party
    boolean cmt;
    long totalMillis;
    int throughputBMT; // calls per second
    int throughputCMT; // calls per second
    int one; // time in msecs to do one call
    private boolean transactional;
    private long prepareDelay;
    private boolean iiop;
    private boolean verbose;
    private boolean showHeader;
    private boolean useHtml;

    public Result(String namingProvider, boolean local, int threadCount, int numberOfCalls, int enlist, boolean iiop,
                  boolean cmt, boolean transactional, long prepareDelay, boolean verbose, boolean showHeader,
                  boolean useHtml, String storeType) {
        this.namingProvider = namingProvider;
        this.local = local;
        this.threadCount = threadCount;
        this.numberOfCalls = numberOfCalls;
        this.enlist = enlist;
        this.cmt = cmt;
        this.prepareDelay = prepareDelay;
        this.totalMillis = this.throughputBMT = this.throughputCMT = 0;
        this.errorCount = 0;
        this.transactional = transactional;
        this.iiop = iiop;
        this.verbose = verbose;
        this.showHeader = showHeader;
        this.useHtml = useHtml;
        this.storeType = storeType;
    }

    public Result(Result result) {
        this(result.namingProvider, result.local, result.threadCount, result.numberOfCalls, result.enlist, result.iiop, result.cmt,
                result.transactional, result.prepareDelay, result.verbose, true, true, result.storeType);

        this.totalMillis = result.totalMillis;
        this.throughputBMT = result.throughputBMT;
        this.throughputCMT = result.throughputCMT;
        this.errorCount = 0;

    }

    public void setProductVersion(String productVersion) {
        this.productVersion = productVersion;
    }

    public void setPatchedJacorb(String patchedJacorb) {
        this.patchedJacorb = patchedJacorb;
    }

    public String getNamingProvider() {
        return namingProvider;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public int getNumberOfCalls() {
        return numberOfCalls;
    }

    public void setNumberOfCalls(int numberOfCalls) {
        this.numberOfCalls = numberOfCalls;
    }

    public int getEnlist() {
        return enlist;
    }

    public boolean isLocal() {
        return local;
    }

    public long getTotalMillis() {
        return totalMillis;
    }

    public void setStoreType(String storeType) {
        this.storeType = storeType;
    }

    public void setTotalMillis(long totalMillis) {
        this.totalMillis = totalMillis;
        if (totalMillis != 0) {
            this.one = totalMillis > 0 ? (int) (totalMillis / numberOfCalls) : 0;
            if (cmt)
                this.throughputCMT = (int) ((1000 * numberOfCalls) / totalMillis);
            else
                this.throughputBMT = (int) ((1000 * numberOfCalls) / totalMillis);
        }
    }

    public int getThroughputCMT() {
        return throughputCMT;
    }

    public int getThroughputBMT() {
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

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
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

    public boolean isVerbose() {
        return verbose;
    }

    public boolean isUseHtml() {
        return useHtml;
    }

    public static Result getDefaultOpts() {
        return new Result("localhost:1199", false, 1, 100, 1, true, false, true, 0, false, true, true, "Unknown");
    }

    public static Result validateOpts(Result opts) {
        if (opts == null)
            opts = getDefaultOpts();
        else if (opts.threadCount < 1)
            opts.threadCount = 1;

        return opts;
    }

    public String toString() {
        if (useHtml)
            return toHtml();
        else
            return String.format("%16s %11d %6d %9d %9s %11d %13b %6d %6b %12s%n",
                productVersion, getThroughputBMT(), getNumberOfCalls(), getErrorCount(), patchedJacorb,
                threadCount, transactional, enlist, !isLocal(), storeType);
    }

    public static StringBuilder getHeaderAsText(StringBuilder sb) {
        return sb.append(String.format("%16s %11s %6s %9s %9s %11s %13s %6s %6s %12s%n",
                "Version", "Throughput", "Calls", "Errors", "Patched",
                "Threads", "Transaction", "Enlist", "Remote", "StoreType"));
    }

    public String toHtml() {
        if (verbose) {
            return String.format(
                    "<tr>\n<td>%d</td>\n<td>%d</td>\n<td>%d</td>\n<td>%b (%b)</td>\n<td>%b</td>\n<td>%d</td>\n<td>%d (%d)</td>\n</tr>\n",
                    getNumberOfCalls(), getErrorCount(), getThreadCount(), isTransactional(), getEnlist() > 0, !isLocal(),
                    getTotalMillis(), getThroughputBMT(), getThroughputCMT()
            );
        } else {
            return String.format("%d ", getThroughputBMT());
        }
    }

    public String getHeader() {
        if (!showHeader)
            return "";
        else if (useHtml)
            return getHeaderAsHtml(new StringBuilder()).toString();
        else
            return getHeaderAsText(new StringBuilder()).toString();
    }

    public static StringBuilder getHeaderAsHtml(StringBuilder sb) {
        return sb.append(
                "<html><body>\n"
                        + "<table>\n"
                        + "<th>Calls</th>\n"
                        + "<th>Errors</th>\n"
                        + "<th>Threads</th>\n"
                        + "<th>Txn (Enlist)</th>\n"
                        + "<th>Local</th>\n"
                        + "<th>Time (ms)</th>\n"
                        + "<th>Throughput BMT</th>\n");
    }

    private static int getIntegerParameter(Map<String, String[]> opts, String name, int deValue) {
        try {
            if (opts.containsKey(name))
                return Integer.parseInt(opts.get(name)[0]);
        } catch (NumberFormatException e) {
            // ignore
        }

        return deValue;
    }
    private static String getStringParameter(Map<String, String[]> opts, String name, String deValue) {
        try {
            if (opts.containsKey(name))
                return opts.get(name)[0];
        } catch (NumberFormatException e) {
            // ignore
        }

        return deValue;
    }
    private static boolean getBooleanParameter(Map<String, String[]> opts, String name, boolean deValue) {
        try {
            if (opts.containsKey(name))
                return Boolean.parseBoolean(opts.get(name)[0]);
        } catch (NumberFormatException e) {
            // ignore
        }

        return deValue;
    }
    public static Result toResult(Map<String, String[]> opts) {
        Result res = validateOpts(new Result(
                getStringParameter(opts, "naming_ep", "localhost:3628"),
                getBooleanParameter(opts, "local", false),
                getIntegerParameter(opts, "threads", 1),
                getIntegerParameter(opts, "count", 100),
                getIntegerParameter(opts, "enlist", 1),
                getBooleanParameter(opts, "iiop", true) ,
                getBooleanParameter(opts, "cmt", true),
                getBooleanParameter(opts, "transactional", true),
                getIntegerParameter(opts, "prepareDelay", 0),
                getBooleanParameter(opts, "verbose", true),
                getBooleanParameter(opts, "show_header", true),
                getBooleanParameter(opts, "html", true),
                getStringParameter(opts, "store_type", "Unknown")));

        res.setProductVersion(getStringParameter(opts, "version", "Unknown"));
        if (!opts.containsKey("jacorb_patch"))
            res.setPatchedJacorb("Unknown");
        else
            res.setPatchedJacorb(Boolean.toString(getBooleanParameter(opts, "jacorb_patch", false)));

        return res;
    }
}
