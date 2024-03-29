package org.gluu.service.document.store.conf;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * @author Yuriy Movchan on 04/10/2020
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JcaDocumentStoreConfiguration implements Serializable {

    private static final long serialVersionUID = 3380170170265842427L;

    private String serverUrl; // http://localhost:8080/rmi
    private String workspaceName;
	private int connectionTimeout; 
    
    private String userId;
    private String password;

    @JsonIgnore
    private String decryptedPassword;

    public String getServerUrl() {
		return serverUrl;
	}

	public void setServerUrl(String serverUrl) {
		this.serverUrl = serverUrl;
	}

	public String getWorkspaceName() {
		return workspaceName;
	}

	public void setWorkspaceName(String workspaceName) {
		this.workspaceName = workspaceName;
	}

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

	public String getDecryptedPassword() {
		return decryptedPassword;
	}

	public void setDecryptedPassword(String decryptedPassword) {
		this.decryptedPassword = decryptedPassword;
	}

	@Override
	public String toString() {
		return "JcaDocumentStoreConfiguration [serverUrl=" + serverUrl + ", workspaceName=" + workspaceName + ", connectionTimeout="
				+ connectionTimeout + ", userId=" + userId + "]";
	}

}
