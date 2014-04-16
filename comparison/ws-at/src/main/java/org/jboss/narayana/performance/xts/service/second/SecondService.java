package org.jboss.narayana.performance.xts.service.second;

import javax.ejb.Remote;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

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
