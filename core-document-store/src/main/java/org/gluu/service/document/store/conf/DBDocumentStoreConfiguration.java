package org.gluu.service.document.store.conf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

import org.gluu.persist.PersistenceEntryManager;

/**
 * @author shekhar L. on 27/04/2022
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DBDocumentStoreConfiguration implements Serializable {

	private static final long serialVersionUID = 3380170170265842538L;

	private String server; // http://localhost:8080
	private String useSSL;
	private int connectionTimeout;
	private PersistenceEntryManager persistenceEntryManager = null;
	private String maxconnections;

	private String userId;
	private String password;
	//private String decryptedPassword;


	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public String getUseSSL() {
		return useSSL;
	}

	public void setUseSSL(String useSSL) {
		this.useSSL = useSSL;
	}

	public String getMaxconnections() {
		return maxconnections;
	}

	public void setMaxconnections(String maxconnections) {
		this.maxconnections = maxconnections;
	}

	public PersistenceEntryManager getPersistenceEntryManager() {
		return persistenceEntryManager;
	}

	public void setPersistenceEntryManager(PersistenceEntryManager persistenceEntryManager) {
		this.persistenceEntryManager = persistenceEntryManager;
	}

	@Override
	public String toString() {
		return "DBDocumentStoreConfiguration [serverUrl=" + server  + ", connectionTimeout="
				+ connectionTimeout + ", userId=" + userId + "]";
	}

}
