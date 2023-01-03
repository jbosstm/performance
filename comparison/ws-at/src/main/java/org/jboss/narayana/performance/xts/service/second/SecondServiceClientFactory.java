package org.jboss.narayana.performance.xts.service.second;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import jakarta.xml.ws.Service;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class SecondServiceClientFactory {

    public static SecondService getInstance() throws MalformedURLException {
        final URL wsdlLocation = new URL("http://localhost:8380/second-service-deployment/SecondServiceService/SecondService?wsdl");
        final QName serviceName = new QName("http://www.narayana.io", "SecondServiceService");
        final QName portName = new QName("http://www.narayana.io", "SecondService");

        final Service service = Service.create(wsdlLocation, serviceName);
        SecondService client = service.getPort(portName, SecondService.class);

        return client;
    }

}
