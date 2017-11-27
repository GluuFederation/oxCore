/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.model.custom.script;

import org.gluu.site.ldap.persistence.annotation.LdapEnum;
import org.xdi.model.custom.script.model.CustomScript;
import org.xdi.model.custom.script.model.auth.AuthenticationCustomScript;
import org.xdi.model.custom.script.type.BaseExternalType;
import org.xdi.model.custom.script.type.auth.DummyPersonAuthenticationType;
import org.xdi.model.custom.script.type.auth.PersonAuthenticationType;
import org.xdi.model.custom.script.type.client.ClientRegistrationType;
import org.xdi.model.custom.script.type.client.DummyClientRegistrationType;
import org.xdi.model.custom.script.type.id.DummyIdGeneratorType;
import org.xdi.model.custom.script.type.id.IdGeneratorType;
import org.xdi.model.custom.script.type.scim.DummyScimType;
import org.xdi.model.custom.script.type.scim.ScimType;
import org.xdi.model.custom.script.type.scope.DummyDynamicScopeType;
import org.xdi.model.custom.script.type.scope.DynamicScopeType;
import org.xdi.model.custom.script.type.session.ApplicationSessionType;
import org.xdi.model.custom.script.type.session.DummyApplicationSessionType;
import org.xdi.model.custom.script.type.uma.UmaClaimsGatheringType;
import org.xdi.model.custom.script.type.uma.UmaDummyClaimsGatheringType;
import org.xdi.model.custom.script.type.uma.UmaDummyRptPolicyType;
import org.xdi.model.custom.script.type.uma.UmaRptPolicyType;
import org.xdi.model.custom.script.type.authz.ConsentGatheringType;
import org.xdi.model.custom.script.type.authz.DummyConsentGatheringType;
import org.xdi.model.custom.script.type.user.*;

import java.util.HashMap;
import java.util.Map;

/**
 * List of supported custom scripts
 *
 * @author Yuriy Movchan Date: 11/11/2014
 */
public enum CustomScriptType implements LdapEnum {
	
	PERSON_AUTHENTICATION("person_authentication", "Person Authentication", PersonAuthenticationType.class, AuthenticationCustomScript.class, "PersonAuthentication", new DummyPersonAuthenticationType()),
	APPLICATION_SESSION("application_session", "Application Session", ApplicationSessionType.class, CustomScript.class, "ApplicationSession", new DummyApplicationSessionType()),
	CACHE_REFRESH("cache_refresh", "Cache Refresh", CacheRefreshType.class, CustomScript.class, "CacheRefresh", new DummyCacheRefreshType()),
	UPDATE_USER("update_user", "Update User", UpdateUserType.class, CustomScript.class, "UpdateUser", new DummyUpdateUserType()),
	USER_REGISTRATION("user_registration", "User Registration", UserRegistrationType.class, CustomScript.class, "UserRegistration", new DummyUserRegistrationType()),
	CLIENT_REGISTRATION("client_registration", "Client Registration", ClientRegistrationType.class, CustomScript.class, "ClientRegistration", new DummyClientRegistrationType()),
	ID_GENERATOR("id_generator", "Id Generator", IdGeneratorType.class, CustomScript.class, "IdGenerator", new DummyIdGeneratorType()),
	UMA_RPT_POLICY("uma_rpt_policy", "UMA RPT Policies", UmaRptPolicyType.class, CustomScript.class, "UmaRptPolicy", new UmaDummyRptPolicyType()),
	UMA_CLAIMS_GATHERING("uma_claims_gathering", "UMA Claims Gathering", UmaClaimsGatheringType.class, CustomScript.class, "UmaClaimsGathering", new UmaDummyClaimsGatheringType()),
	CONSENT_GATHERING("consent_gathering", "Consent Gathering", ConsentGatheringType.class, CustomScript.class, "ConsentGathering", new DummyConsentGatheringType()),
	DYNAMIC_SCOPE("dynamic_scope", "Dynamic Scopes", DynamicScopeType.class, CustomScript.class, "DynamicScope", new DummyDynamicScopeType()),
	SCIM("scim", "SCIM", ScimType.class, CustomScript.class, "ScimEventHandler", new DummyScimType());

	private String value;
	private String displayName;
	private Class<? extends BaseExternalType> customScriptType;
	private Class<? extends CustomScript> customScriptModel;
	private String pythonClass;
	private BaseExternalType defaultImplementation;
	
	private static Map<String, CustomScriptType> mapByValues = new HashMap<String, CustomScriptType>();
	static {
		for (CustomScriptType enumType : values()) {
			mapByValues.put(enumType.getValue(), enumType);
		}
	}

	private CustomScriptType(String value, String displayName, Class<? extends BaseExternalType> customScriptType, Class<? extends CustomScript> customScriptModel, String pythonClass, BaseExternalType defaultImplementation) {
		this.displayName = displayName;
		this.value = value;
		this.customScriptType = customScriptType;
		this.customScriptModel = customScriptModel;
		this.pythonClass = pythonClass;
		this.defaultImplementation = defaultImplementation;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getValue() {
		return value;
	}

	public Class<? extends BaseExternalType> getCustomScriptType() {
		return customScriptType;
	}

	public Class<? extends CustomScript> getCustomScriptModel() {
		return customScriptModel;
	}

	public String getPythonClass() {
		return pythonClass;
	}

	public BaseExternalType getDefaultImplementation() {
		return defaultImplementation;
	}

	public static CustomScriptType getByValue(String value) {
		return mapByValues.get(value);
	}

	public Enum<? extends LdapEnum> resolveByValue(String value) {
		return getByValue(value);
	}

	@Override
	public String toString() {
		return value;
	}

}
