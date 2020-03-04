package com.hp.mwtests.ts.jts.orbspecific.local.performance;

import com.arjuna.ats.internal.jts.ORBManager;
import com.arjuna.orbportability.OA;
import com.arjuna.orbportability.ORB;
import com.arjuna.orbportability.RootOA;
import io.narayana.perf.WorkerLifecycle;
import org.omg.CORBA.ORBPackage.InvalidName;

import static org.junit.Assert.fail;

public class PerformanceWorkerLifecycle<Void> implements WorkerLifecycle<Void> {
    ORB myORB = null;
    RootOA myOA = null;

    protected void initCorba() {
        myORB = ORB.getInstance("test");

        myOA = OA.getRootOA(myORB);

        myORB.initORB(new String[]{}, null);

        try {
            myOA.initOA();
        } catch (InvalidName invalidName) {
            fail(invalidName.getMessage());
        }

        ORBManager.setORB(myORB);
        ORBManager.setPOA(myOA);
    }

    @Override
    public void init() {
        initCorba();
    }

    @Override
    public void fini() {
        myOA.destroy();
        myORB.shutdown();
    }
}
