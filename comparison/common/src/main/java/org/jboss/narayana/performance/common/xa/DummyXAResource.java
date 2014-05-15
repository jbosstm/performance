package org.jboss.narayana.performance.common.xa;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class DummyXAResource implements XAResource {

    private static final Logger LOG = Logger.getLogger(DummyXAResource.class);

    private String name;

    public DummyXAResource(String name) {
        if (LOG.isTraceEnabled()) {
            LOG.tracev("DummyXAResource(name={0})", name);
        }

        this.name = name;
    }

    public void commit(Xid arg0, boolean arg1) throws XAException {
        if (LOG.isTraceEnabled()) {
            LOG.tracev("DummyXAResource.commit(): name={0}", name);
        }
    }

    public void end(Xid arg0, int arg1) throws XAException {
        if (LOG.isTraceEnabled()) {
            LOG.tracev("DummyXAResource.end(): name={0}", name);
        }
    }

    public void forget(Xid arg0) throws XAException {
        if (LOG.isTraceEnabled()) {
            LOG.tracev("DummyXAResource.forget(): name={0}", name);
        }
    }

    public int getTransactionTimeout() throws XAException {
        if (LOG.isTraceEnabled()) {
            LOG.tracev("DummyXAResource.getTransactionTimeout(): name={0}", name);
        }

        return 0;
    }

    public boolean isSameRM(XAResource arg0) throws XAException {
        if (LOG.isTraceEnabled()) {
            LOG.tracev("DummyXAResource.isSameRM(): name={0}", name);
        }

        return this == arg0;
    }

    public int prepare(Xid arg0) throws XAException {
        if (LOG.isTraceEnabled()) {
            LOG.tracev("DummyXAResource.prepare(): name={0}", name);
        }

        return 0;
    }

    public Xid[] recover(int arg0) throws XAException {
        if (LOG.isTraceEnabled()) {
            LOG.tracev("DummyXAResource.recover(): name={0}", name);
        }

        return null;
    }

    public void rollback(Xid arg0) throws XAException {
        if (LOG.isTraceEnabled()) {
            LOG.tracev("DummyXAResource.rollback(): name={0}", name);
        }
    }

    public boolean setTransactionTimeout(int arg0) throws XAException {
        if (LOG.isTraceEnabled()) {
            LOG.tracev("DummyXAResource.setTransactionTimeout(): name={0}", name);
        }

        return false;
    }

    public void start(Xid arg0, int arg1) throws XAException {
        if (LOG.isTraceEnabled()) {
            LOG.tracev("DummyXAResource.start(): name={0}", name);
        }
    }
}
