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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Base64;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.eclipse.jetty.server.session.JDBCSessionDataStore;
import org.eclipse.jetty.server.session.SessionContext;
import org.eclipse.jetty.server.session.SessionData;
import org.eclipse.jetty.server.session.UnreadableSessionDataException;
import org.eclipse.jetty.util.ClassLoadingObjectInputStream;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JDBCExtendedSessionDataStore
 *
 * Session data stored in database
 */
@ManagedObject
public class JDBCExtendedSessionDataStore extends JDBCSessionDataStore
{
    private static final Logger LOG = LoggerFactory.getLogger(JDBCExtendedSessionDataStore.class);

	private int _lockPeriodMillis;
	private int _delayPeriodMillis;
	private boolean _compressSerializedData = false;

    /**
     * SessionTableSchema
     */
    public static class SessionTableSchema extends org.eclipse.jetty.server.session.JDBCSessionDataStore.SessionTableSchema
    {
        protected String _lockTimeColumn = "lockTime";

        protected void setDatabaseAdaptor(DatabaseAdaptor dbadaptor)
        {
            _dbAdaptor = dbadaptor;
        }

        public void setLockTimeColumn(String lockTimeColumn)
        {
            checkNotNull(lockTimeColumn);
            _lockTimeColumn = lockTimeColumn;
        }

        public String getLockTimeColumn()
        {
            return _lockTimeColumn;
        }

        private void checkNotNull(String s)
        {
            if (s == null)
                throw new IllegalArgumentException(s);
        }

        private String getSchemaTableName()
        {
            return (getSchemaName() != null ? getSchemaName() + "." : "") + getTableName();
        }

        /*
         * Added lockTableColumn creation
         */
        public String getCreateStatementAsString()
        {
            if (_dbAdaptor == null)
                throw new IllegalStateException("No DBAdaptor");

            String blobType = _dbAdaptor.getBlobType();
            String longType = _dbAdaptor.getLongType();
            String stringType = _dbAdaptor.getStringType();

            return "create table " + getSchemaTableName() + " (" + _idColumn + " " + stringType + "(120), " +
                _contextPathColumn + " " + stringType + "(60), " + _virtualHostColumn + " " + stringType + "(60), " + _lastNodeColumn + " " + stringType + "(60), " + _accessTimeColumn + " " + longType + ", " +
                _lastAccessTimeColumn + " " + longType + ", " + _createTimeColumn + " " + longType + ", " + _cookieTimeColumn + " " + longType + ", " +
                _lastSavedTimeColumn + " " + longType + ", " + _expiryTimeColumn + " " + longType + ", " + _maxIntervalColumn + " " + longType + ", "  + _lockTimeColumn + " " + longType + ", " +
                _mapColumn + " " + blobType + ", primary key(" + _idColumn + ", " + _contextPathColumn + "," + _virtualHostColumn + "))";
        }

        /*
         * Added lockTableColumn value set
         */
        @Override
        public String getInsertSessionStatementAsString()
        {
            return "insert into " + getSchemaTableName() +
                " (" + getIdColumn() + ", " + getContextPathColumn() + ", " + getVirtualHostColumn() + ", " + getLastNodeColumn() +
                ", " + getAccessTimeColumn() + ", " + getLastAccessTimeColumn() + ", " + getCreateTimeColumn() + ", " + getCookieTimeColumn() +
                ", " + getLastSavedTimeColumn() + ", " + getExpiryTimeColumn() + ", " + getMaxIntervalColumn() + ", " + getLockTimeColumn() + ", " + getMapColumn() + ") " +
                " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        }

        /*
         * Added lockTableColumn value clear
         */
        @Override
        public PreparedStatement getUpdateSessionStatement(Connection connection, String id, SessionContext context)
            throws SQLException
        {
            String s = "update " + getSchemaTableName() +
                " set " + getLastNodeColumn() + " = ?, " + getAccessTimeColumn() + " = ?, " +
                getLastAccessTimeColumn() + " = ?, " + getLastSavedTimeColumn() + " = ?, " + getExpiryTimeColumn() + " = ?, " +
                getMaxIntervalColumn() + " = ?, " + getLockTimeColumn() + " = ?, " + getMapColumn() + " = ? where " + getIdColumn() + " = ? and " + getContextPathColumn() +
                " = ? and " + getVirtualHostColumn() + " = ?";

            String cp = context.getCanonicalContextPath();
            if (_dbAdaptor.isEmptyStringNull() && StringUtil.isBlank(cp))
                cp = NULL_CONTEXT_PATH;

            PreparedStatement statement = connection.prepareStatement(s);
            statement.setString(9, id);
            statement.setString(10, cp);
            statement.setString(11, context.getVhost());
            return statement;
        }

        @Override
        public String toString()
        {
            return String.format("%s[%s]", super.toString(), _lockTimeColumn);
        }
    }

    /*
     * Quickly insert with lockTime. After that do update with session data
     */
    @Override
    protected void doInsert(String id, SessionData data)
            throws Exception
        {
    		if (LOG.isDebugEnabled()) {
    			LOG.debug(">>>>>>>>>> INSERT START: {}", id);
    		}
        	boolean useLock = _lockPeriodMillis > 0;
            String s = _sessionTableSchema.getInsertSessionStatementAsString();

            try (Connection connection = ((org.eclipse.jetty.server.session.extended.DatabaseAdaptor) _dbAdaptor).getConnection())
            {
                connection.setAutoCommit(true);
                try (PreparedStatement statement = connection.prepareStatement(s))
                {
                    statement.setString(1, id); //session id

                    String cp = _context.getCanonicalContextPath();
                    if (_dbAdaptor.isEmptyStringNull() && StringUtil.isBlank(cp))
                        cp = NULL_CONTEXT_PATH;

                    statement.setString(2, cp); //context path

                    statement.setString(3, _context.getVhost()); //first vhost
                    statement.setString(4, data.getLastNode()); //my node id
                    statement.setLong(5, data.getAccessed()); //accessTime
                    statement.setLong(6, data.getLastAccessed()); //lastAccessTime
                    statement.setLong(7, data.getCreated()); //time created
                    statement.setLong(8, data.getCookieSet()); //time cookie was set
                    statement.setLong(9, data.getLastSaved()); //last saved time
                    statement.setLong(10, data.getExpiry());
                    statement.setLong(11, data.getMaxInactiveMs());
                    statement.setLong(12, System.currentTimeMillis()); // lockTime

                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ObjectOutputStream oos = new ExtendedObjectOutputStream(baos))
                       {
                    	   if (useLock) {
                    		   // Write empty legacy object to speed up initial record insert
                    		   oos.writeObject(new HashMap<String, Object>());
                    	   } else {
                               SessionData.serializeAttributes(data, oos);
                    	   }
                           byte[] bytes = baos.toByteArray();
                           if (LOG.isDebugEnabled()) {
                        	   LOG.debug("SessionData dump in INSERT for Vhost {} in base64: {}", _context.getVhost(), Base64.getEncoder().encodeToString(bytes));
                           }

                           if (_compressSerializedData) {
                               try (ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
                            		GZIPOutputStream gos = new GZIPOutputStream(baos2)) {
	                       			gos.write(bytes);
	                          	    gos.finish();
	                       			bytes = baos2.toByteArray();
                    	       }
                    	   }
                           try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes))
                           {
                               statement.setBinaryStream(13, bais, bytes.length); //attribute map as blob
                           }
                       }

                       statement.executeUpdate();
                       if (LOG.isDebugEnabled())
                           LOG.debug("Inserted session {}", data);
                   }
                }
    			if (LOG.isDebugEnabled()) {
    				LOG.debug("<<<<<<<<<< INSERT END: {}", id);
    			}

        		if (useLock) {
        			// Save with serialized data
        			doUpdate(id, data);
        		}
        }

        @Override
        protected void doUpdate(String id, SessionData data)
            throws Exception
        {
        	if (LOG.isDebugEnabled()) {
        		LOG.debug(">>>>>>>>>> UPDATE START: {}", id);
        	}
            try (Connection connection = ((org.eclipse.jetty.server.session.extended.DatabaseAdaptor) _dbAdaptor).getConnection())
            {
                connection.setAutoCommit(true);
                try (PreparedStatement statement = _sessionTableSchema.getUpdateSessionStatement(connection, data.getId(), _context))
                {
                    statement.setString(1, data.getLastNode()); //should be my node id
                    statement.setLong(2, data.getAccessed()); //accessTime
                    statement.setLong(3, data.getLastAccessed()); //lastAccessTime
                    statement.setLong(4, data.getLastSaved()); //last saved time
                    statement.setLong(5, data.getExpiry());
                    statement.setLong(6, data.getMaxInactiveMs());
                    statement.setNull(7, Types.BIGINT);

                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                         ObjectOutputStream oos = new ExtendedObjectOutputStream(baos))
                    {
                        SessionData.serializeAttributes(data, oos);
                        byte[] bytes = baos.toByteArray();
                        if (LOG.isDebugEnabled()) {
                     	   LOG.debug("SessionData dump in UPDATE for Vhost {} in base64: {}", _context.getVhost(), Base64.getEncoder().encodeToString(bytes));
                        }

                        if (_compressSerializedData) {
                            try (ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
                         		GZIPOutputStream gos = new GZIPOutputStream(baos2)) {
	                       			gos.write(bytes);
	                          	    gos.finish();
	                       			bytes = baos2.toByteArray();
                 	       }
                 	    }
                        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes))
                        {
                            statement.setBinaryStream(8, bais, bytes.length); //attribute map as blob
                        }
                    }

                    statement.executeUpdate();

                    if (LOG.isDebugEnabled())
                        LOG.debug("Updated session {}", data);
                }
            }
            if (LOG.isDebugEnabled()) {
            	LOG.debug("<<<<<<<<<< UPDATE END: {}", id);
            }
        }

        @Override
        public SessionData doLoad(String id) throws Exception
        {
        	if (LOG.isDebugEnabled()) {
        		LOG.debug(">>>>>>>>>> LOAD START: {}", id);
        	}
        	ExtendedSessionData extendedSessionData = doLoadImpl(id, true);
        	if (extendedSessionData.getLockTime() == null) {
            	return extendedSessionData.getSessionData();
        	}

        	// Wait specified milliseconds and try to load again
        	long unlockTime = extendedSessionData.getLockTime() + _lockPeriodMillis;
        	long currTime = System.currentTimeMillis();
        	long sleepTime = unlockTime - currTime;
        	if (sleepTime > 0) {
        		if (LOG.isDebugEnabled()) {
        			LOG.debug("<<<<<<<<<< LOAD START DELAY FOR LOCK: {}", id);
        		}
	        	Thread.sleep(sleepTime);
	        	if (LOG.isDebugEnabled()) {
	        		LOG.debug(">>>>>>>>>> LOAD END DELAY FOR LOCK: {}", id);
	        	}
        	}

        	// Load after lock expiration
        	extendedSessionData = doLoadImpl(id, false);

        	if (LOG.isDebugEnabled()) {
        		LOG.debug("<<<<<<<<<< LOAD END: {}", id);
        	}
        	return extendedSessionData.getSessionData();
        }

        protected ExtendedSessionData doLoadImpl(String id, boolean checkLock) throws Exception
        {
        	if (LOG.isDebugEnabled()) {
        		LOG.debug("---------- LOAD IMPL: {} : {}", id, checkLock);
        	}
            try (Connection connection = ((org.eclipse.jetty.server.session.extended.DatabaseAdaptor) _dbAdaptor).getConnection();
                 PreparedStatement statement = _sessionTableSchema.getLoadStatement(connection, id, _context);
                 ResultSet result = statement.executeQuery())
            {
                SessionData data = null;
                Long lockTime = null;
                if (result.next())
                {
                    data = newSessionData(id,
                        result.getLong(_sessionTableSchema.getCreateTimeColumn()),
                        result.getLong(_sessionTableSchema.getAccessTimeColumn()),
                        result.getLong(_sessionTableSchema.getLastAccessTimeColumn()),
                        result.getLong(_sessionTableSchema.getMaxIntervalColumn()));
                    data.setCookieSet(result.getLong(_sessionTableSchema.getCookieTimeColumn()));
                    data.setLastNode(result.getString(_sessionTableSchema.getLastNodeColumn()));
                    data.setLastSaved(result.getLong(_sessionTableSchema.getLastSavedTimeColumn()));
                    data.setExpiry(result.getLong(_sessionTableSchema.getExpiryTimeColumn()));
                    data.setContextPath(_context.getCanonicalContextPath());
                    data.setVhost(_context.getVhost());

                    lockTime = result.getLong(((org.eclipse.jetty.server.session.extended.JDBCExtendedSessionDataStore.SessionTableSchema) _sessionTableSchema).getLockTimeColumn());
                    
                    // Check lock if needed
                    if (checkLock && !isLockExpired(lockTime)) {
                    	return new ExtendedSessionData(data, lockTime);
                    }

                    InputStream is2 = _dbAdaptor.getBlobInputStream(result, _sessionTableSchema.getMapColumn());
                    
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    int nRead;
                    byte[] buf = new byte[16384];

                    while ((nRead = is2.read(buf, 0, buf.length)) != -1) {
                      buffer.write(buf, 0, nRead);
                    }

                    if (LOG.isDebugEnabled()) {
                    	LOG.debug("Data for deserialization in base64: {}", Base64.getEncoder().encodeToString(buffer.toByteArray()));
                    }

                    try (InputStream is = getBlobInputStream(result);
                         ClassLoadingObjectInputStream ois = new ClassLoadingObjectInputStream(is))
                    {
                    	if (LOG.isDebugEnabled()) {
                    		LOG.debug(">>>>>>>>>> DESERIALIZATION START: {}", id);
                    	}
                    	if (_delayPeriodMillis > 0) {
                    		if (LOG.isDebugEnabled()) {
                    			LOG.debug(">>>>>>>>>> DESERIALIZATION DELAY: {}", id);
                    		}
            	        	Thread.sleep(_delayPeriodMillis);
            	        	if (LOG.isDebugEnabled()) {
            	        		LOG.debug("<<<<<<<<<< DESERIALIZATION RESUME: {}", id);
            	        	}
                    	}
                        SessionData.deserializeAttributes(data, ois);
                        if (LOG.isDebugEnabled()) {
                        	LOG.debug("<<<<<<<<<< DESERIALIZATION END: {}", id);
                        }
                    }
                    catch (Exception e)
                    {
                        throw new UnreadableSessionDataException(id, _context, e);
                    }

                    if (LOG.isDebugEnabled())
                        LOG.debug("LOADED session {}", data);
                }
                else if (LOG.isDebugEnabled()) {
                    LOG.debug("No session {}", id);
                }

                return new ExtendedSessionData(data, null);
            }
        }

        private InputStream getBlobInputStream(ResultSet resultSet) throws SQLException, IOException {
        	InputStream resultStream =_dbAdaptor.getBlobInputStream(resultSet, _sessionTableSchema.getMapColumn());
        	if (_compressSerializedData) {
        		LOG.info("USING COMPRESSION");
        		try (InputStream gis = new GZIPInputStream(resultStream)) {
        			resultStream = new ByteArrayInputStream(gis.readAllBytes());
        		}
        	}

        	return resultStream;
        }
        
        private boolean isLockExpired(Long lockTime) {
        	if (lockTime == null) {
        		return true;
        	}

        	long unlockTime = lockTime + _lockPeriodMillis;
        	long currTime = System.currentTimeMillis();

        	return unlockTime < currTime;
		}

		public static class ExtendedSessionData {
        	private final SessionData sessionData;
        	private final Long lockTime;

        	public ExtendedSessionData(SessionData sessionData, Long lockTime) {
				this.sessionData = sessionData;
				this.lockTime = lockTime;
			}

			public SessionData getSessionData() {
				return sessionData;
			}

			public Long getLockTime() {
				return lockTime;
			}
        	
        }

        @ManagedAttribute(value = "interval in millis to wait for session record lock", readonly = true)
        public int getLockPeriodMillis()
        {
            return _lockPeriodMillis;
        }

        public void setLockPeriodMillis(int millis)
        {
            _lockPeriodMillis = millis;
        }

        @ManagedAttribute(value = "interval in millis to wait before session deserialization", readonly = true)
        public int getDelayPeriodMillis()
        {
            return _delayPeriodMillis;
        }

        public void setDelayPeriodMillis(int millis)
        {
        	_delayPeriodMillis = millis;
        }

        @ManagedAttribute(value = "specify if serialized data should be compressed", readonly = true)
		public boolean getCompressSerializedData() {
			return _compressSerializedData;
		}

		public void setCompressSerializedData(boolean compressSerializedData) {
			_compressSerializedData = compressSerializedData;
		}

}
