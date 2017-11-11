/*
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
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
 */
package com.hp.mwtests.ts.arjuna.atomicaction;

import com.arjuna.ats.arjuna.AtomicAction;
import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.ats.arjuna.coordinator.SynchronizationRecord;
import com.arjuna.ats.arjuna.utils.ThreadUtil;
import com.arjuna.ats.internal.arjuna.thread.ThreadActionData;
import com.hp.mwtests.ts.arjuna.JMHConfigCore;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.*;

//@State(Scope.Benchmark)
public class CheckedActionTest {
    @State(Scope.Benchmark)
    public static class BenchmarkState {
        SynchronizationRecord synch = null;

        public BenchmarkState() {
            synch = new SynchronizationRecord() {
                private Uid uid = new Uid();

                @Override
                public Uid get_uid() {return uid;}

                @Override
                public boolean beforeCompletion() {return true;}

                @Override
                public boolean afterCompletion(int status) {return true;}

                @Override
                public boolean isInterposed() {return false;}

                @Override
                public int compareTo(Object o) {
                    SynchronizationRecord other = (SynchronizationRecord) o;

                    if(this.isInterposed() && (!other.isInterposed()))
                        return 1;
                    else if((!this.isInterposed()) && other.isInterposed())
                        return -1;
                    else if(this.uid.equals(other.get_uid()))
                        return 0;
                    else
                        return this.uid.lessThan(other.get_uid()) ? -1 : 1;
                }
            };
        }

        AtomicAction[] newActions(int count) {
            AtomicAction[] actions = new  AtomicAction[count];

            for (int i = 0; i < count; i++)
                actions[i] = new AtomicAction();

            return actions;
        }
    }

    @Benchmark
    public boolean testCheckedAction (BenchmarkState state)
    {
        int actionCount = 10;
        AtomicAction[] actions = state.newActions(actionCount);
        Thread[] threads = {new Thread(), new Thread(), new Thread(), new Thread(), new Thread(), };

        for (int i = 0; i < actionCount; i++) {
            actions[i].begin();
            actions[i].addSynchronization(state.synch);

            for (int j = 0; j < threads.length; j++)
                actions[i].addChildThread(threads[j]);

            AtomicAction.suspend();
        }

        for (int i = actionCount - 1; i >= 0; i--) {
            AtomicAction.resume(actions[i]);

            for (int j = 0; j < threads.length; j++)
                actions[i].removeChildThread(ThreadUtil.getThreadId(threads[j]));

            actions[i].commit();
        }

        return true;
    }

    @Benchmark
    public boolean testThreadActionData (BenchmarkState state) {

        AtomicAction A = new AtomicAction();
        AtomicAction B = new AtomicAction();

        A.begin();
        B.begin();

        if (ThreadActionData.currentAction() == null)
            System.out.printf("testThreadActionData currentAction() FAILED%n");

        ThreadActionData.restoreActions(A);

        if (ThreadActionData.popAction() != A)
             System.out.printf("testThreadActionData popAction() FAILED%n");

        ThreadActionData.purgeActions(Thread.currentThread());

        ThreadActionData.popAction(Thread.currentThread().getName());

        B.commit();
        A.commit();

        return true;
    }

    public static void main(String[] args) throws RunnerException, CommandLineOptionException {
        JMHConfigCore.runJTABenchmark(CheckedActionTest.class.getSimpleName(), args);
    }
}
