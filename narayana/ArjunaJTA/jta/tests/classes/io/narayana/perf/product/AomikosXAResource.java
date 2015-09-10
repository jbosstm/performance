/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
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
