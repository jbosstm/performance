package org.jboss.narayana.performance.jts.service.first;

import java.rmi.RemoteException;

import javax.ejb.EJBHome;

public interface FirstEJBHome extends EJBHome {

    FirstEJB create() throws RemoteException;

}
