/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package narayana.performance.ejb;

import narayana.performance.util.Result;
import java.rmi.RemoteException;
import javax.ejb.EJBObject;

public interface WorkerEJB extends EJBObject {
	public Result doWork(Result opts) throws RemoteException;
}