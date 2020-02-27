/*
 * Copyright 2015, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.narayana.rts;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.jboss.jbossts.star.provider.HttpResponseException;
import org.jboss.jbossts.star.util.TxLinkNames;
import org.jboss.jbossts.star.util.TxMediaType;
import org.jboss.jbossts.star.util.TxStatus;
import org.jboss.jbossts.star.util.TxSupport;
import org.jboss.logging.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;

import javax.ws.rs.Path;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.PathParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.PUT;
import javax.ws.rs.HEAD;
import javax.ws.rs.QueryParam;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Link;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An example of how a REST resource can act as a participant in a REST Atomic transaction.
 * For a complete implementation of a participant please refer to the test suite, in particular the inner class:
 * org.jboss.jbossts.star.test.BaseTest$TransactionalResource which implements all the responsibilities of a participant
 * <p>
 * The example sends a service request which is handled by the method someServiceRequest. The request includes the
 * URL for registering durable participants within the transaction. This naive implementation assumes every request
 * with a valid enlistment URL is a request a new unit of transactional work and enlists a new URL into the transaction.
 * Thus if a client makes two http requests to the method someServiceRequest then the participant is enlisted twice
 * into the transaction but with different completion URLs. This facilitates the demonstration of 2 phase commit
 * processing.
 */
@Path(TransactionAwareResource.PSEGMENT)
public class TransactionAwareResource {
    protected static final Logger log = Logger.getLogger(TxnTest.class);
    public static final String PSEGMENT = "service";
    //    private static AtomicInteger workId = new AtomicInteger(0);
    private static AtomicInteger commitCnt = new AtomicInteger(0);
    static final String NON_TXN_MSG = "non transactional request";
    static Client client = null;

    private static Client createClient() {
        ClientConnectionManager cm = new ThreadSafeClientConnManager();
        HttpClient httpClient = new DefaultHttpClient(cm);
        ApacheHttpClient4Engine engine = new ApacheHttpClient4Engine(httpClient);

        return new ResteasyClientBuilder().httpEngine(engine).build();
    }

    static Client getClient() {
        synchronized (NON_TXN_MSG) {
            if (client == null)
                client = createClient(); //ClientBuilder.newClient();
        }

        return client;
    }


    @ApplicationPath("eg")
    public static class ServiceApp extends Application {
        @Override
        public Set<Class<?>> getClasses() {
            HashSet<Class<?>> classes = new HashSet<>();
            classes.add(TransactionAwareResource.class);

            return classes;
        }
    }

    @GET
    @Produces("text/plain")
    public Response someServiceRequest(@Context UriInfo info, @QueryParam("tid") String wid, @QueryParam("enlistURL") String enlistUrl) {
        if (enlistUrl == null || enlistUrl.length() == 0)
            return Response.ok(NON_TXN_MSG).build();

//        String wid = tid; // Integer.toString(workId.incrementAndGet());
        String serviceURL = info.getBaseUri() + info.getPath().substring(1); // avoid '//' in urls
        String linkHeader = makeTwoPhaseAwareParticipantLinkHeader(
                serviceURL, false, String.valueOf(wid), null);
        Response response;

        // enlist using linkHeader
        try {
            boolean jc = false;

            if (log.isTraceEnabled()) {
                log.tracef("[%s]: workId %s enlist%n", wid, wid);
                log.tracef("\tLink: %s%n", linkHeader);
            }

            if (jc) {
                response = getClient().target(enlistUrl).request().header("Link", linkHeader).post(Entity.entity(new Form(), TxMediaType.POST_MEDIA_TYPE));

                if (log.isTraceEnabled())
                    log.tracef("[%s]: workId %s enlisted%n", wid, wid);
                if (response.getStatus() != HttpURLConnection.HTTP_CREATED)
                    return Response.status(response.getStatus()).build();
            } else {
                enlist(enlistUrl, linkHeader);
            }
            response = Response.ok(wid).build();
            if (log.isTraceEnabled())
                log.tracef("[%s]: workId %s returning%n", wid, wid);
            return response;
        } catch (HttpResponseException e) {
            return Response.status(e.getActualResponse()).build();
        } catch (Throwable e) {
            return Response.serverError().build();
        }
    }

    private String enlist(String enlistUrl, String linkHeader) {
        Map<String, String> reqHeaders = new HashMap<>();

        reqHeaders.put("Link", linkHeader);

        return new TxSupport().httpRequest(new int[]{HttpURLConnection.HTTP_CREATED}, enlistUrl, "POST",
                TxMediaType.POST_MEDIA_TYPE, null, null, reqHeaders);
    }

    @GET
    @Path("query")
    public Response someServiceRequest() {
        return Response.ok(Integer.toString(commitCnt.intValue())).build();
    }

    /*
     * this method handles PUT requests to the url that the participant gave to the REST Atomic Transactions
     * implementation (in the someServiceRequest method). This is the endpoint that the transaction manager interacts
     * with when it needs participants to prepare/commit/rollback their transactional work.
     */
    @PUT
    @Path("{wId}/terminator")
    public Response terminate(@PathParam("wId") @DefaultValue("") String wId, String content) {
//        System.out.println("Service: PUT request to terminate url: wId=" + wId + ", status:=" + content);
        TxStatus status = TxSupport.toTxStatus(content);

        if (log.isTraceEnabled())
            log.tracef("terminate workId %s enlist%n", wId);
        if (status.isCommit()) {
            commitCnt.incrementAndGet();
        } else if (!status.isPrepare() && !status.isAbort()) {
            return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).build();
        }
        if (log.isTraceEnabled())
            log.tracef("terminated workId %s enlist%n", wId);
        return Response.ok(TxSupport.toStatusContent(status.name())).build();
    }

    @HEAD
    @Path("{pId}/participant")
    public Response getTerminator(@Context UriInfo info, @PathParam("pId") @DefaultValue("") String wId) {
        String serviceURL = info.getBaseUri() + info.getPath();

        String linkHeader = makeTwoPhaseAwareParticipantLinkHeader(serviceURL, false, wId, null);

        return Response.ok().header("Link", linkHeader).build();
    }

    public String makeTwoPhaseAwareParticipantLinkHeader(
            String baseURI, boolean vParticipant, String uid1, String uid2) {

        StringBuilder resourcePrefix = new StringBuilder(baseURI);

        if (uid1 != null)
            resourcePrefix.append('/').append(uid1);
        if (uid2 != null)
            resourcePrefix.append('/').append(uid2);

        resourcePrefix.append('/');

        StringBuilder participantLinkHeader = new StringBuilder(
                Link.fromUri(resourcePrefix + TxLinkNames.PARTICIPANT_RESOURCE).rel(TxLinkNames.PARTICIPANT_RESOURCE).build().toString()).
                append(',').
                append(Link.fromUri(resourcePrefix + TxLinkNames.PARTICIPANT_TERMINATOR).rel(TxLinkNames.PARTICIPANT_TERMINATOR).build());

        if (vParticipant)
            participantLinkHeader.append(',').
                    append(Link.fromUri(resourcePrefix + TxLinkNames.VOLATILE_PARTICIPANT).rel(TxLinkNames.VOLATILE_PARTICIPANT).build());


        return participantLinkHeader.toString();
    }
}

