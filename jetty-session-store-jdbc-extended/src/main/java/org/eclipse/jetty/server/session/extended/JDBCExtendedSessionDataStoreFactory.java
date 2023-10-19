//
// ========================================================================
// Copyright (c) 1995 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v. 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
// which is available at https://www.apache.org/licenses/LICENSE-2.0.
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package org.eclipse.jetty.server.session.extended;

import org.eclipse.jetty.server.session.DatabaseAdaptor;
import org.eclipse.jetty.server.session.JDBCSessionDataStore;
import org.eclipse.jetty.server.session.JDBCSessionDataStoreFactory;
import org.eclipse.jetty.server.session.SessionDataStore;
import org.eclipse.jetty.server.session.SessionHandler;

/**
 * JDBCExtendedSessionDataStoreFactory
 */
public class JDBCExtendedSessionDataStoreFactory extends JDBCSessionDataStoreFactory
{

    public static final int DEFAULT_LOCK_PERIOD_MILLIS = 0; //default of 0 millis
    public static final int DEFAULT_DELAY_PERIOD_MILLIS = 0; //default of 0 millis

	DatabaseAdaptor _adaptor;

    JDBCSessionDataStore.SessionTableSchema _schema;

	private int _lockPeriodMillis = DEFAULT_LOCK_PERIOD_MILLIS;
	private int _delayPeriodMillis = DEFAULT_DELAY_PERIOD_MILLIS;
	private boolean _compressSerializedData = false;

    @Override
    public SessionDataStore getSessionDataStore(SessionHandler handler)
    {
    	JDBCExtendedSessionDataStore ds = new JDBCExtendedSessionDataStore();
        ds.setDatabaseAdaptor(_adaptor);
        ds.setSessionTableSchema(_schema);
        ds.setGracePeriodSec(getGracePeriodSec());
        ds.setSavePeriodSec(getSavePeriodSec());
        
        ds.setLockPeriodMillis(_lockPeriodMillis);
        ds.setDelayPeriodMillis(_delayPeriodMillis);
        ds.setCompressSerializedData(_compressSerializedData);
        return ds;
    }

    /**
     * @param adaptor the {@link DatabaseAdaptor} to set
     */
    public void setDatabaseAdaptor(DatabaseAdaptor adaptor)
    {
        _adaptor = adaptor;
    }

    /**
     * @param schema the {@link JDBCSessionDataStoreFactory} to set
     */
    public void setSessionTableSchema(JDBCSessionDataStore.SessionTableSchema schema)
    {
        _schema = schema;
    }

    public int getLockPeriodMillis()
    {
        return _lockPeriodMillis;
    }

    public void setLockPeriodMillis(int millis)
    {
        _lockPeriodMillis = millis;
    }

	public int getDelayPeriodMillis() {
		return _delayPeriodMillis;
	}

	public void setDelayPeriodMillis(int millis) {
		this._delayPeriodMillis = millis;
	}

	public boolean getCompressSerializedData() {
		return _compressSerializedData;
	}

	public void setCompressSerializedData(boolean compressSerializedData) {
		_compressSerializedData = compressSerializedData;
	}
}
