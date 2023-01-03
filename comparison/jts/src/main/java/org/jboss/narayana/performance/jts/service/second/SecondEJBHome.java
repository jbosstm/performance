package org.jboss.narayana.performance.jts.service.second;

import java.rmi.RemoteException;

import jakarta.ejb.EJBHome;

public interface SecondEJBHome extends EJBHome {

    SecondEJB create() throws RemoteException;

}
