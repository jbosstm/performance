package narayana.performance.web;


import narayana.performance.beans.HelloWorld;
import narayana.performance.beans.PerformanceTester;
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
    private static final String jndiName = "HelloWorldJNDIName";

    private HelloWorld localBean;

    private PerformanceTester tester;

    @Override
    public void init() throws ServletException {
        super.init();

        tester = new PerformanceTester(getServletContext().getInitParameter("max_threads"),
                getServletContext().getInitParameter("batch_size"));

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
        tester.fini();
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");

        try {
            tester.measureThroughput(response.getWriter(), localBean, Result.toResult(request.getParameterMap()));
        } catch (Exception e) {
            throw new ServletException(e.getCause());
        }
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
