/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 *
 * (C) 2005-2006,
 * @author JBoss Inc.
 */
/*
 * Copyright (C) 1998, 1999, 2000,
 *
 * Hewlett-Packard Arjuna Labs,
 * Newcastle upon Tyne,
 * Tyne and Wear,
 * UK.
 *
 * $Id: Performance1.java 2342 2006-03-30 13:06:17Z  $
 */

package com.hp.mwtests.ts.arjuna.performance;

import com.arjuna.ats.arjuna.AtomicAction;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.internal.arjuna.objectstore.VolatileStore;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;
import com.hp.mwtests.ts.arjuna.JMHConfigCore;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.CommandLineOptionException;

//@Warmup(iterations = JMHConfigCore.WI, time = JMHConfigCore.WT)
// , timeUnit = JMHConfigCore.WTU)
//@Measurement(iterations = JMHConfigCore.MI, time = JMHConfigCore.MT)
// , timeUnit = JMHConfigCore.MTU)
//@Fork(JMHConfigCore.BF)
//@Threads(JMHConfigCore.BT)
public class Performance1 {
    @State(Scope.Thread)
    public static class BenchmarkState {
        private BasicRecord record1 = new BasicRecord();
        private BasicRecord record2 = new BasicRecord();
    }

    ;

    static {
        try {
            BeanPopulator.getDefaultInstance(ObjectStoreEnvironmentBean.class).setObjectStoreType(VolatileStore.class.getName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    public boolean onePhase(BenchmarkState benchmarkState) {
        AtomicAction A = new AtomicAction();

        A.begin();
        A.add(benchmarkState.record1);
        A.commit();

        return true;
    }

    @Benchmark
    public boolean twoPhase(BenchmarkState benchmarkState) {
        AtomicAction A = new AtomicAction();

        A.begin();
        A.add(benchmarkState.record1);
        A.add(benchmarkState.record2);
        A.commit();

        return true;
    }

    public static void main(String[] args) throws RunnerException, CommandLineOptionException {
        JMHConfigCore.runJTABenchmark(Performance1.class.getSimpleName(), args);
    }
}
