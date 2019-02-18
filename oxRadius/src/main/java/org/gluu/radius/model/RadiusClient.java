/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.radius.model;

import java.io.Serializable;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapDN;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;

/**
 * @author Bonaventure Djeumen <rolain@gluu.org>
 */

@LdapEntry
@LdapObjectClass(values={"oxRadiusClient","top"})
public class RadiusClient implements Serializable {

	private static final long serialVersionUID = -3145075159422463151L;
	
	@LdapDN
	private String dn;

	@LdapAttribute(name="inum")
	private String inum;

	@LdapAttribute(name="oxRadiusClientName")
	private String name;

	@LdapAttribute(name="oxRadiusClientIpAddress")
	private String ipAddress;

	@LdapAttribute(name="oxRadiusClientSecret")
	private String secret;


	public RadiusClient() {

		this.dn = null;
		this.inum = null;
		this.name = null;
		this.ipAddress = null;
		this.secret = null;
	}

	public RadiusClient(String name,String ipaddress,String secret) {

		this.dn = null;
		this.inum = null;
		this.name = name;
		this.ipAddress = ipaddress;
		this.secret = secret;
	}


	public String getDn() {

		return this.dn;
	}

	public RadiusClient setDn(String dn) {

		this.dn = dn;
		return this;
	}


	public String getInum() {

		return this.inum;
	}

	public RadiusClient setInum(String inum) {

		this.inum = inum;
		return this;
	}


	public String getName() {

		return this.name;
	}

	public RadiusClient setName(String name) {

		this.name = name;
		return this;
	}


	public String getIpAddress() {

		return this.ipAddress;
	}

	public RadiusClient setIpAddress(String ipaddress) {

		this.ipAddress = ipaddress;
		return this;
	}


	public String getSecret() {

		return this.secret;
	}

	public RadiusClient setSecret(String secret) {

		this.secret = secret;
		return this;
	}
}