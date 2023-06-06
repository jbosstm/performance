/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package narayana.performance.ejb;

import java.rmi.RemoteException;
import javax.ejb.EJBHome;

public interface WorkerEJBHome extends EJBHome {
    public WorkerEJB create() throws RemoteException;
}