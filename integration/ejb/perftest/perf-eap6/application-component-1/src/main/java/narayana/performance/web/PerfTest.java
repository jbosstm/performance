package narayana.performance.web;


import narayana.performance.beans.HelloWorld;
import narayana.performance.beans.HelloWorldBean;
import narayana.performance.beans.PerformanceTester;
import narayana.performance.util.Lookup;
import narayana.performance.util.Result;

import javax.ejb.EJB;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class PerfTest extends HttpServlet {
    private static final String jndiName = "java:app/perf-eap6-app-component-1/HelloWorldBean";

    @EJB(lookup = jndiName)
    private HelloWorld localBean;

    private PerformanceTester tester;

    @Override
    public void init() throws ServletException {
        super.init();

        tester = new PerformanceTester(getServletContext().getInitParameter("max_threads"),
                getServletContext().getInitParameter("batch_size"));

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

