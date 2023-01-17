/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.config.oxtrust;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.apache.commons.lang3.builder.DiffBuilder;
import org.apache.commons.lang3.builder.DiffResult;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.gluu.model.ldap.GluuLdapConfiguration;

import javax.enterprise.inject.Vetoed;
import java.util.List;

/**
 * Cache refresh configuration
 *
 * @author Yuriy Movchan Date: 07.13.2011
 */
@Vetoed
@JsonIgnoreProperties(ignoreUnknown = true)
public class CacheRefreshConfiguration implements Configuration {

    private List<GluuLdapConfiguration> sourceConfigs;
    private GluuLdapConfiguration inumConfig;
    private GluuLdapConfiguration targetConfig;

    private int ldapSearchSizeLimit;

    private List<String> keyAttributes;
    private List<String> keyObjectClasses;
    private List<String> sourceAttributes;

    private String customLdapFilter;

    private String updateMethod;

    private boolean defaultInumServer;

    private boolean keepExternalPerson;

    private boolean useSearchLimit;

    private List<CacheRefreshAttributeMapping> attributeMapping;

    private String snapshotFolder;
    private int snapshotMaxCount;

    public List<GluuLdapConfiguration> getSourceConfigs() {
        return sourceConfigs;
    }

    public void setSourceConfigs(List<GluuLdapConfiguration> sourceConfigs) {
        this.sourceConfigs = sourceConfigs;
    }

    public GluuLdapConfiguration getInumConfig() {
        return inumConfig;
    }

    public void setInumConfig(GluuLdapConfiguration inumConfig) {
        this.inumConfig = inumConfig;
    }

    public GluuLdapConfiguration getTargetConfig() {
        return targetConfig;
    }

    public void setTargetConfig(GluuLdapConfiguration targetConfig) {
        this.targetConfig = targetConfig;
    }

    public int getLdapSearchSizeLimit() {
        return ldapSearchSizeLimit;
    }

    public void setLdapSearchSizeLimit(int ldapSearchSizeLimit) {
        this.ldapSearchSizeLimit = ldapSearchSizeLimit;
    }

    public List<String> getKeyAttributes() {
        return keyAttributes;
    }

    public void setKeyAttributes(List<String> keyAttributes) {
        this.keyAttributes = keyAttributes;
    }

    public List<String> getKeyObjectClasses() {
        return keyObjectClasses;
    }

    public void setKeyObjectClasses(List<String> keyObjectClasses) {
        this.keyObjectClasses = keyObjectClasses;
    }

    public List<String> getSourceAttributes() {
        return sourceAttributes;
    }

    public void setSourceAttributes(List<String> sourceAttributes) {
        this.sourceAttributes = sourceAttributes;
    }

    public String getCustomLdapFilter() {
        return customLdapFilter;
    }

    public void setCustomLdapFilter(String customLdapFilter) {
        this.customLdapFilter = customLdapFilter;
    }

    public String getUpdateMethod() {
        return updateMethod;
    }

    public void setUpdateMethod(String updateMethod) {
        this.updateMethod = updateMethod;
    }

    public boolean isKeepExternalPerson() {
        return keepExternalPerson;
    }

    public void setKeepExternalPerson(boolean keepExternalPerson) {
        this.keepExternalPerson = keepExternalPerson;
    }

    public boolean isDefaultInumServer() {
        return defaultInumServer;
    }

    public void setDefaultInumServer(boolean defaultInumServer) {
        this.defaultInumServer = defaultInumServer;
    }

    public boolean isUseSearchLimit() {
        return useSearchLimit;
    }

    public void setUseSearchLimit(boolean useSearchLimit) {
        this.useSearchLimit = useSearchLimit;
    }

    public List<CacheRefreshAttributeMapping> getAttributeMapping() {
        return attributeMapping;
    }

    public void setAttributeMapping(List<CacheRefreshAttributeMapping> attributeMapping) {
        this.attributeMapping = attributeMapping;
    }

    public String getSnapshotFolder() {
        return snapshotFolder;
    }

    public void setSnapshotFolder(String snapshotFolder) {
        this.snapshotFolder = snapshotFolder;
    }

    public int getSnapshotMaxCount() {
        return snapshotMaxCount;
    }

    public void setSnapshotMaxCount(int snapshotMaxCount) {
        this.snapshotMaxCount = snapshotMaxCount;
    }

    @Override
	public DiffResult diff(Configuration newObj) {
    	CacheRefreshConfiguration obj = (CacheRefreshConfiguration) newObj;
		 return new DiffBuilder(this, obj, ToStringStyle.SHORT_PREFIX_STYLE)
			        .append("sourceConfigs", this.sourceConfigs, obj.sourceConfigs)
			        .append("inumConfig", this.inumConfig, obj.inumConfig)
			        .append("targetConfig", this.targetConfig, obj.targetConfig)
			        .append("ldapSearchSizeLimit", this.ldapSearchSizeLimit, obj.ldapSearchSizeLimit)
			        .append("keyAttributes", this.keyAttributes, obj.keyAttributes)
			        .append("keyObjectClasses", this.keyObjectClasses, obj.keyObjectClasses)
			        .append("sourceAttributes", this.sourceAttributes, obj.sourceAttributes)
			        .append("customLdapFilter", this.customLdapFilter, obj.customLdapFilter)
			        .append("updateMethod", this.updateMethod, obj.updateMethod)
			        .append("defaultInumServer", this.defaultInumServer, obj.defaultInumServer)
			        .append("keepExternalPerson", this.keepExternalPerson, obj.keepExternalPerson)
			        .append("useSearchLimit", this.useSearchLimit, obj.useSearchLimit)
			        .append("attributeMapping", this.attributeMapping, obj.attributeMapping)
			        .append("snapshotFolder", this.snapshotFolder, obj.snapshotFolder)
			        .append("snapshotMaxCount", this.snapshotMaxCount, obj.snapshotMaxCount)			        
			        .build();
	}

}
