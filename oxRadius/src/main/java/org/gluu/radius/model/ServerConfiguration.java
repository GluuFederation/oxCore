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


@LdapEntry
@LdapObjectClass(values={"oxRadiusServerConfiguration","top"})
public class ServerConfiguration implements Serializable {

	private static final long serialVersionUID = 7998195679159211451L;
	
	public static final String LISTEN_ON_ALL_INTERFACES = "0.0.0.0";
	public static final Integer DEFAULT_RADIUS_AUTH_PORT = 1812;
	public static final Integer DEFAULT_RADIUS_ACCT_PORT = 1813;


	@LdapDN
	private String  dn;
	
	@LdapAttribute(name="oxRadiusListenInterface")
	private String  listenInterface;

	@LdapAttribute(name="oxRadiusAuthenticationPort")
	private Integer authPort;

	@LdapAttribute(name="oxRadiusAccountingPort")
	private Integer acctPort;

	@LdapAttribute(name="oxRadiusOpenidUsername")
	private String  openidUsername;

	@LdapAttribute(name="oxRadiusOpenidPassword")
	private String  openidPassword;


	@LdapAttribute(name="oxRadiusOpenIdBaseUrl")
	private String openidBaseUrl;


	public ServerConfiguration() {

		this.dn = null;
		this.listenInterface = LISTEN_ON_ALL_INTERFACES;
		this.authPort = DEFAULT_RADIUS_AUTH_PORT;
		this.acctPort = DEFAULT_RADIUS_ACCT_PORT;
		this.openidUsername = null;
		this.openidPassword = null;
		this.openidBaseUrl = null;
	}


	public ServerConfiguration(String listeninterface,Integer authport,Integer acctport,String openidusername, 
		String openidpassword) {

		this.dn = null;
		this.listenInterface = listeninterface;
		this.authPort = authport;
		this.acctPort = acctport;
		this.openidUsername = openidusername;
		this.openidPassword = openidpassword;
		this.openidBaseUrl  = null;
	}


	public String getDn() {

		return this.dn;
	}

	public ServerConfiguration setDn(String dn) {

		this.dn = dn;
		return this;
	}

	public String getListenInterface() {

		return this.listenInterface;
	}

	public ServerConfiguration setListenInterface(String listeninterface) {

		this.listenInterface = listeninterface;
		return this;
	}

	public Integer getAuthPort() {

		return this.authPort;
	}

	public ServerConfiguration setAuthPort(Integer authport) {

		this.authPort = authPort;
		return this;
	}

	public Integer getAcctPort() {

		return this.acctPort;
	}

	public ServerConfiguration setAcctPort(Integer acctPort) {

		this.acctPort = acctPort;
		return this;
	}

	public String getOpenidUsername() {

		return this.openidUsername;
	}

	public ServerConfiguration setOpenidUsername(String openidusername) {

		this.openidUsername = openidusername;
		return this;
	}


	public String getOpenidPassword() {

		return this.openidPassword;
	}


	public ServerConfiguration setOpenidPassword(String openidpassword) {

		this.openidPassword = openidpassword;
		return this;
	}

	public String getOpenidBaseUrl() {

		return this.openidBaseUrl;
	}

	public ServerConfiguration setOpenidBaseUrl(String openidBaseUrl) {

		this.openidBaseUrl = openidBaseUrl;
		return this;
	}
}