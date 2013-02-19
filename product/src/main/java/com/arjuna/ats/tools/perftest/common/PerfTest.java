package com.arjuna.ats.tools.perftest.common;

public interface PerfTest extends Runnable
{
    long getResult();

    Exception getException();
}
