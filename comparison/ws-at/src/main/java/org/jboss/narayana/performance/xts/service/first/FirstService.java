package org.jboss.narayana.performance.xts.service.first;

import jakarta.ejb.Remote;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@Remote
@SOAPBinding(style = SOAPBinding.Style.RPC)
@WebService(name = "FirstService", targetNamespace = "http://www.narayana.io")
public interface FirstService {

    @WebMethod
    public void execute();

}
