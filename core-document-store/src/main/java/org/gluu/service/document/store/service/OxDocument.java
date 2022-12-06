/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.service.document.store.service;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.gluu.persist.model.base.Entry;
import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.annotation.DataEntry;
import org.gluu.persist.annotation.ObjectClass;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Group
 * 
 * @author Yuriy Movchan Date: 11.02.2010
 */
@DataEntry(sortBy = { "displayName" })
@ObjectClass(value = "oxDocument")
@JsonInclude(Include.NON_NULL)
public class OxDocument extends Entry implements Serializable {

	private static final long serialVersionUID = -2812480357430436503L;

	private transient boolean selected;
//inum $ displayName $ description $ document $ oxModuleProperty $ oxLevel $ oxRevision $ oxEnabled $ oxAlias )
	@AttributeName(ignoreDuringUpdate = true)
	private String inum;


	@AttributeName
	private String displayName;

	@AttributeName
	private String description;

	@AttributeName
	private String document;
	
	@AttributeName
	private Date creationDate;
	
	@AttributeName
	private List<String> oxModuleProperty;

	@AttributeName
	private String oxLevel;

	@AttributeName
	private String oxRevision;
	
	@AttributeName
	private Boolean oxEnabled;
	
	@AttributeName
	private String oxAlias;

	public String getInum() {
		return inum;
	}

	public void setInum(String inum) {
		this.inum = inum;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public String getDocument() {
		return document;
	}

	public void setDocument(String document) {
		this.document = document;
	}

	public List<String> getOxModuleProperty() {
		return oxModuleProperty;
	}

	public void setOxModuleProperty(List<String> oxModuleProperty) {
		this.oxModuleProperty = oxModuleProperty;
	}

	public String getOxLevel() {
		return oxLevel;
	}

	public void setOxLevel(String oxLevel) {
		this.oxLevel = oxLevel;
	}

	public String getOxRevision() {
		return oxRevision;
	}

	public void setOxRevision(String oxRevision) {
		this.oxRevision = oxRevision;
	}

	public Boolean getOxEnabled() {
		return oxEnabled;
	}

	public void setOxEnabled(Boolean oxEnabled) {
		this.oxEnabled = oxEnabled;
	}

	public String getOxAlias() {
		return oxAlias;
	}

	public void setOxAlias(String oxAlias) {
		this.oxAlias = oxAlias;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

}
