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

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int count = getIntegerParameter(request, "count", 100);

        testResults.addFirst(localPerfTestBean.testBMTTxns(new Result(count, true, 1199)));
        testResults.addFirst(localPerfTestBean.testBMTTxns(new Result(count, true, 0)));
        testResults.addFirst(localPerfTestBean.testBMTTxns(new Result(count, false, 1199)));
        testResults.addFirst(localPerfTestBean.testBMTTxns(new Result(count, false, 0)));

        while (testResults.size() > 16)
            testResults.removeLast();

        response.setContentType("text/html;charset=UTF-8");

        PrintWriter out = response.getWriter();

        out.println("<html><body>");

        out.printf("<table>\n");
        out.printf("<th>Calls</th>\n");
        out.printf("<th>Enlist</th>\n");
        out.printf("<th>Local</th>\n");
        out.printf("<th>Time (ms)</th>\n");
        out.printf("<th>Throughput</th>\n");

        int i = 0;
        for (Result result : testResults) {
            if (i++ % 4 == 0)
                out.printf("<tr><td>&nbsp;</td></tr>\n");

            out.printf("<tr>\n");
            out.printf("<td>%d</td>\n", result.getNumberOfCalls());
            out.printf("<td>%b</td>\n", result.isEnlist());
            out.printf("<td>%b</td>\n", result.isLocal());
            out.printf("<td>%d</td>\n", result.getTotalMillis());
            out.printf("<td>%d (%d)</td>\n", result.getThroughput(), result.getOne());
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
