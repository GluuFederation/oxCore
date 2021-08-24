package org.gluu.config.oxtrust;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;


@JsonIgnoreProperties(ignoreUnknown = true)
public class ScimProperties implements Serializable {

    /**
    *
    */
    private static final long serialVersionUID = -5154249316054593386L;

    private int maxCount;
    
    private ScimMode protectionMode;
    
    private String userExtensionSchemaURI;

    public int getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }
    
    public ScimMode getProtectionMode() {
    	return protectionMode;
    }
    
    public void setProtectionMode(ScimMode mode) {
    	protectionMode = mode;
    }

    public String getUserExtensionSchemaURI() {
        return userExtensionSchemaURI;
    }

    public void setUserExtensionSchemaURI(String userExtensionSchemaURI) {
        this.userExtensionSchemaURI = userExtensionSchemaURI;
    }

}
