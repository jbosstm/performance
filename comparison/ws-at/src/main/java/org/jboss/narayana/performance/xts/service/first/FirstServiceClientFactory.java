package org.jboss.narayana.performance.xts.service.first;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class FirstServiceClientFactory {

    public static FirstService getInstance() throws MalformedURLException {
        final URL wsdlLocation = new URL("http://localhost:8180/first-service-deployment/FirstServiceService/FirstService?wsdl");
        final QName serviceName = new QName("http://www.narayana.io", "FirstServiceService");
        final QName portName = new QName("http://www.narayana.io", "FirstService");

        final Service service = Service.create(wsdlLocation, serviceName);
        FirstService client = service.getPort(portName, FirstService.class);

        return client;
    }

}
