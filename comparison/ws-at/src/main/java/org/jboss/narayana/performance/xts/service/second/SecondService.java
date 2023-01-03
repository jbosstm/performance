package org.jboss.narayana.performance.xts.service.second;

import jakarta.ejb.Remote;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@Remote
@SOAPBinding(style = SOAPBinding.Style.RPC)
@WebService(name = "SecondService", targetNamespace = "http://www.narayana.io")
public interface SecondService {

    @WebMethod
    public void execute();

}
