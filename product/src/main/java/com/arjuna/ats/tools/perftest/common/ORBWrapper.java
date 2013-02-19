package com.arjuna.ats.tools.perftest.common;

//import com.arjuna.ats.arjuna.common.RecoveryEnvironmentBean;
//import com.arjuna.ats.arjuna.common.recoveryPropertyManager;
import com.arjuna.ats.internal.jts.ORBManager;
import com.arjuna.ats.internal.jts.orbspecific.recovery.RecoveryEnablement;
import com.arjuna.orbportability.OA;
import com.arjuna.orbportability.ORB;
import com.arjuna.orbportability.RootOA;
import com.arjuna.orbportability.common.opPropertyManager;
import com.arjuna.orbportability.internal.utils.PostInitLoader;
import org.omg.CORBA.ORBPackage.InvalidName;

import java.util.Map;

/**
 * class to start and stop an ORB for use with OTS based transactions
 */
public class ORBWrapper
{
    private final static String ORB_NAME = "testorb";

    private ORB orb;
    private RootOA oa;

    public void start()
    {

        //           new RecoveryEnablement().startRCservice();
        com.arjuna.ats.internal.arjuna.recovery.RecoveryManagerImple rm =
                new com.arjuna.ats.internal.arjuna.recovery.RecoveryManagerImple(true);

        m3();
    }

/*
     not available in 4.6.1.GA
    private void m1() {
        System.setProperty("jacorb.implname", "arjuna");

        ORBManager.reset();

        RecoveryEnablement rec = new RecoveryEnablement();

        rec.startRCservice();
    }

    private void m2() {
        Map<String, String> properties = opPropertyManager.getOrbPortabilityEnvironmentBean().getOrbInitializationProperties();

        properties.put( PostInitLoader.generateORBPropertyName("com.arjuna.orbportability.orb", ORB_NAME), "com.arjuna.ats.jts.utils.ORBSetup");

        opPropertyManager.getOrbPortabilityEnvironmentBean().setOrbInitializationProperties(properties);


        orb = ORB.getInstance(ORB_NAME);
        oa = OA.getRootOA(orb);
        orb.initORB(new String[] {}, null);
        try {
            oa.initOA();
        } catch (InvalidName invalidName) {
            throw new RuntimeException(invalidName);
        }

    } */

    private void m3() {
        String[] args = {};
        System.setProperty( PostInitLoader.generateORBPropertyName("com.arjuna.orbportability.orb", ORB_NAME), "com.arjuna.ats.jts.utils.ORBSetup");
        orb = ORB.getInstance(ORB_NAME);
        oa = OA.getRootOA(orb);
        orb.initORB(args, null);
        try {
            oa.initOA();
        } catch (InvalidName invalidName) {
            throw new RuntimeException(invalidName);
        }
    }

    public void stop()
    {
        if (oa != null) oa.destroy();
        if (orb != null) orb.shutdown();
    }
}
