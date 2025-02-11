/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package org.jboss.narayana.rts;

import org.jboss.jbossts.star.util.TxLinkNames;
import org.jboss.jbossts.star.util.TxMediaType;
import org.jboss.jbossts.star.util.TxStatusMediaType;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Set;

public class TxnHelper {
    public static final int NO_OF_SVC_CALLS = 2;

    public static Link getLink(Set<Link> links, String relation) {
        for (Link link : links)
            if (link.getRel().equals(relation))
                return link;

        return null;
    }

    public static Set<Link> beginTxn(Client client, String txurl) throws IOException {
        Response response = null;

        try {
            response = client.target(txurl).request().post(
                    Entity.entity(new Form(), MediaType.APPLICATION_FORM_URLENCODED_TYPE));
            Set<Link> links = response.getLinks();

            response.readEntity(String.class);
            if (response.getStatus() != HttpURLConnection.HTTP_CREATED)
                throw new RuntimeException("beginTxn returned " + response.getStatus());

            return links;
        } finally {
            if (response != null)
                response.close();
        }
    }

    public static int endTxn(Client client, Set<Link> links) throws IOException {
        Response response = null;

        try {
            response = client.target(getLink(links, TxLinkNames.TERMINATOR).getUri())
                    .request().put(Entity.entity(TxStatusMediaType.TX_COMMITTED, TxMediaType.TX_STATUS_MEDIA_TYPE));

            int sc = response.getStatus();

            response.readEntity(String.class);

            if (sc != HttpURLConnection.HTTP_OK)
                throw new RuntimeException("endTxn returned " + sc);

            return sc;
        } finally {
            if (response != null)
                response.close();
        }
    }

    public static String sendRequest(int expect, Client client, String serviceUrl) throws IOException {
        return client.target(serviceUrl).request().buildGet().invoke(String.class);

/*        Response response = null;

        try {
            String res = "";

            response = client.target(serviceUrl).request().get();

            if (response.getStatus() != expect)
                throw new RuntimeException("sendRequest returned " + response.getStatus());

            Object entity = response.getEntity();

            if (entity != null) {
                res = entity.toString();
                EntityUtils.consume((HttpEntity) response.getEntity());
            }

            return res;
        } finally {
            if (response != null)
                response.close();
        }*/
    }
}