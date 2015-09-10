package io.narayana.perf.product;

import com.arjuna.ats.jta.xa.performance.XAResourceImpl;
import com.atomikos.datasource.RecoverableResource;
import com.atomikos.datasource.ResourceException;
import com.atomikos.icatch.Participant;
import com.atomikos.icatch.RecoveryService;

public class AomikosXAResource extends XAResourceImpl implements com.atomikos.datasource.RecoverableResource {
    @Override
    public void setRecoveryService(RecoveryService recoveryService) throws ResourceException {

    }

    @Override
    public boolean recover(Participant participant) throws ResourceException {
        return false;
    }

    @Override
    public void endRecovery() throws ResourceException {

    }

    @Override
    public void close() throws ResourceException {

    }

    @Override
    public boolean isSameRM(RecoverableResource recoverableResource) throws ResourceException {
        return recoverableResource.equals(this);
    }

    @Override
    public boolean isClosed() {
        return false;
    }
}
