package org.jboss.narayana.performance.jts.service.second;

import java.rmi.RemoteException;

import jakarta.ejb.EJBObject;

public interface SecondEJB extends EJBObject {

    void execute() throws RemoteException;

}
