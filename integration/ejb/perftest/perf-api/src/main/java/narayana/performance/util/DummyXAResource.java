package narayana.performance.util;

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

	public void commit(Xid arg0, boolean arg1) throws XAException {
//		System.out.printf("committing %s\n", name);
	}

	public void end(Xid arg0, int arg1) throws XAException {
	}

	public void forget(Xid arg0) throws XAException {
	}

	public int getTransactionTimeout() throws XAException {
		return 0;
	}

	public boolean isSameRM(XAResource arg0) throws XAException {
		return this == arg0;
	}

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

	public Xid[] recover(int arg0) throws XAException {
		return null;
	}

	public void rollback(Xid arg0) throws XAException {
	}

	public boolean setTransactionTimeout(int arg0) throws XAException {
		return false;
	}

	public void start(Xid arg0, int arg1) throws XAException {
	}
}
