package org.jboss.as.quickstarts.perf.jts.ejb;

import javax.ejb.Remote;

@Remote
public interface PerfTestBeanRemote {
    Result testBMTTxns(Result result);
    Result testCMTTxns(Result result);
}
