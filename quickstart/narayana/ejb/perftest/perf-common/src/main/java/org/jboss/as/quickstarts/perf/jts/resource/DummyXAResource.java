package org.jboss.as.quickstarts.perf.jts.resource;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

public class DummyXAResource implements XAResource {

	private String name;
    private long delay;

	public DummyXAResource(String name, long delayMillis) {
		this.name = name;
        this.delay = delayMillis;
	}

    public DummyXAResource(String name) {
        this(name, 0L);
    }

	@Override
	public void commit(Xid arg0, boolean arg1) throws XAException {
//		System.out.printf("committing %s\n", name);
	}

	@Override
	public void end(Xid arg0, int arg1) throws XAException {
	}

	@Override
	public void forget(Xid arg0) throws XAException {
	}

	@Override
	public int getTransactionTimeout() throws XAException {
		return 0;
	}

	@Override
	public boolean isSameRM(XAResource arg0) throws XAException {
		return this == arg0;
	}

	@Override
	public int prepare(Xid arg0) throws XAException {
//		System.out.printf("%s prepare (with %dms dela)\n", name, delay);
        if (delay > 0L)
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

        return 0;
	}

	@Override
	public Xid[] recover(int arg0) throws XAException {
		return null;
	}

	@Override
	public void rollback(Xid arg0) throws XAException {
	}

	@Override
	public boolean setTransactionTimeout(int arg0) throws XAException {
		return false;
	}

	@Override
	public void start(Xid arg0, int arg1) throws XAException {
	}
}
