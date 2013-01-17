package org.jboss.as.quickstarts.perf.jts.web;

import org.jboss.as.quickstarts.perf.jts.ejb.Lookup;
import org.jboss.as.quickstarts.perf.jts.ejb.PerfTestBeanRemote;
import org.jboss.as.quickstarts.perf.jts.ejb.Result;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class PerfTest extends HttpServlet {
    private Deque<Result> testResults;

    private PerfTestBeanRemote localPerfTestBean;

    @Override
    public void init() throws ServletException {
        super.init();

        testResults = new ArrayDeque<Result>();

        localPerfTestBean = Lookup.getPerfTestBeanRemote();

        if (localPerfTestBean == null)
            throw new ServletException("error looking up PerfTestBean");
    }

    private int getIntegerParameter(HttpServletRequest request, String name, int deValue) {
        try {
            return Integer.parseInt(request.getParameter(name));
        } catch (NumberFormatException e) {
            return deValue;
        }
    }

    private Result testCMTTxns(Result result) {
        long now = System.currentTimeMillis();
        long nCalls = result.getNumberOfCalls();

        result.setCmt(true);

        for (long i = 0; i < nCalls; i++)
            localPerfTestBean.testCMTTxns(result);

        result.setTotalMillis(System.currentTimeMillis() - now);

        return result;
    }

    private Result testBMTTxns(Result result) {
        result.setCmt(false);

        return localPerfTestBean.testBMTTxns(result);
    }

    private void testTxns(Result result, boolean cmt) {
        if (cmt)
            result = testCMTTxns(result);
        result = testBMTTxns(result);

        testResults.addFirst(result);
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int count = getIntegerParameter(request, "count", 100);
        boolean enlist = getIntegerParameter(request, "enlist", 1) != 0;
        int remote = getIntegerParameter(request, "remote", 1199);
        boolean cmt = getIntegerParameter(request, "cmt", 0) != 0;
        boolean transactional = getIntegerParameter(request, "transactional", 1) != 0;
        int how = getIntegerParameter(request, "how", 0);

        if (how == 0) {
            testTxns(new Result(count, enlist, remote, cmt, transactional), cmt);
        } else {
            testTxns(new Result(count, true, remote, cmt, transactional), cmt);
            testTxns(new Result(count, true, 0, cmt, transactional), cmt);
            testTxns(new Result(count, false, remote, cmt, transactional), cmt);
            testTxns(new Result(count, false, 0, cmt, transactional), cmt);
        }

        while (testResults.size() > 16)
            testResults.removeLast();

        response.setContentType("text/html;charset=UTF-8");

        PrintWriter out = response.getWriter();

        out.println("<html><body>");
        out.printf("<table>\n");
        out.printf("<th>Calls</th>\n");
        out.printf("<th>Errors</th>\n");
        out.printf("<th>Txn (Enlist)</th>\n");
        out.printf("<th>Local</th>\n");
        out.printf("<th>Time (ms)</th>\n");
        out.printf("<th>Throughput BMT (CMT)</th>\n");

        int i = 0;
        for (Result result : testResults) {
            if (i++ % 4 == 0)
                out.printf("<tr><td>&nbsp;</td></tr>\n");

            out.printf("<tr>\n");
            out.printf("<td>%d</td>\n", result.getNumberOfCalls());
            out.printf("<td>%d</td>\n", result.getErrorCount());
            out.printf("<td>%b (%b)</td>\n", result.isTransactional(), result.isEnlist());
            out.printf("<td>%b</td>\n", result.isLocal());
            out.printf("<td>%d</td>\n", result.getTotalMillis());
            out.printf("<td>%d (%d)</td>\n", result.getThroughputBMT(), result.getThroughputCMT());
            out.printf("</tr>\n");
        }

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
