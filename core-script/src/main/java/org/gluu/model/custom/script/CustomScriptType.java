/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.model.custom.script;

import org.gluu.model.custom.script.model.CustomScript;
import org.gluu.model.custom.script.model.auth.AuthenticationCustomScript;
import org.gluu.model.custom.script.type.BaseExternalType;
import org.gluu.model.custom.script.type.auth.DummyPersonAuthenticationType;
import org.gluu.model.custom.script.type.auth.PersonAuthenticationType;
import org.gluu.model.custom.script.type.authz.ConsentGatheringType;
import org.gluu.model.custom.script.type.authz.DummyConsentGatheringType;
import org.gluu.model.custom.script.type.authzchallenge.AuthorizationChallengeType;
import org.gluu.model.custom.script.type.authzchallenge.DummyAuthorizationChallengeType;
import org.gluu.model.custom.script.type.ciba.DummyEndUserNotificationType;
import org.gluu.model.custom.script.type.ciba.EndUserNotificationType;
import org.gluu.model.custom.script.type.client.ClientRegistrationType;
import org.gluu.model.custom.script.type.client.DummyClientRegistrationType;
import org.gluu.model.custom.script.type.fido2.DummyFido2ExtensionType;
import org.gluu.model.custom.script.type.fido2.Fido2ExtensionType;
import org.gluu.model.custom.script.type.id.DummyIdGeneratorType;
import org.gluu.model.custom.script.type.id.IdGeneratorType;
import org.gluu.model.custom.script.type.idp.DummyIdpType;
import org.gluu.model.custom.script.type.idp.IdpType;
import org.gluu.model.custom.script.type.introspection.DummyIntrospectionType;
import org.gluu.model.custom.script.type.introspection.IntrospectionType;
import org.gluu.model.custom.script.type.logout.DummyEndSessionType;
import org.gluu.model.custom.script.type.logout.EndSessionType;
import org.gluu.model.custom.script.type.owner.DummyResourceOwnerPasswordCredentialsType;
import org.gluu.model.custom.script.type.owner.ResourceOwnerPasswordCredentialsType;
import org.gluu.model.custom.script.type.persistence.DummyPeristenceType;
import org.gluu.model.custom.script.type.persistence.PersistenceType;
import org.gluu.model.custom.script.type.postauthn.DummyPostAuthnType;
import org.gluu.model.custom.script.type.postauthn.PostAuthnType;
import org.gluu.model.custom.script.type.revoke.DummyRevokeTokenType;
import org.gluu.model.custom.script.type.revoke.RevokeTokenType;
import org.gluu.model.custom.script.type.scim.DummyScimType;
import org.gluu.model.custom.script.type.scim.ScimType;
import org.gluu.model.custom.script.type.scope.DummyDynamicScopeType;
import org.gluu.model.custom.script.type.scope.DynamicScopeType;
import org.gluu.model.custom.script.type.session.ApplicationSessionType;
import org.gluu.model.custom.script.type.session.DummyApplicationSessionType;
import org.gluu.model.custom.script.type.spontaneous.DummySpontaneousScopeType;
import org.gluu.model.custom.script.type.spontaneous.SpontaneousScopeType;
import org.gluu.model.custom.script.type.token.DummyUpdateTokenType;
import org.gluu.model.custom.script.type.token.UpdateTokenType;
import org.gluu.model.custom.script.type.uma.*;
import org.gluu.model.custom.script.type.user.*;
import org.gluu.persist.annotation.AttributeEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * List of supported custom scripts
 *
 * @author Yuriy Movchan Date: 11/11/2014
 */
public enum CustomScriptType implements AttributeEnum {

    PERSON_AUTHENTICATION("person_authentication", "Person Authentication", PersonAuthenticationType.class, AuthenticationCustomScript.class,
            "PersonAuthentication", new DummyPersonAuthenticationType()),
    AUTHORIZATION_CHALLENGE("authorization_challenge", "Authorization Challenge", AuthorizationChallengeType.class, CustomScript.class, "AuthorizationChallenge", new DummyAuthorizationChallengeType()),
    INTROSPECTION("introspection", "Introspection", IntrospectionType.class, CustomScript.class, "Introspection", new DummyIntrospectionType()),
    RESOURCE_OWNER_PASSWORD_CREDENTIALS("resource_owner_password_credentials", "Resource Owner Password Credentials", ResourceOwnerPasswordCredentialsType.class, CustomScript.class, "ResourceOwnerPasswordCredentials", new DummyResourceOwnerPasswordCredentialsType()),
    APPLICATION_SESSION("application_session", "Application Session", ApplicationSessionType.class, CustomScript.class, "ApplicationSession",
            new DummyApplicationSessionType()),
    CACHE_REFRESH("cache_refresh", "Cache Refresh", CacheRefreshType.class, CustomScript.class, "CacheRefresh",
            new DummyCacheRefreshType()),
    UPDATE_USER("update_user", "Update User", UpdateUserType.class, CustomScript.class, "UpdateUser", new DummyUpdateUserType()),
    USER_REGISTRATION("user_registration", "User Registration", UserRegistrationType.class, CustomScript.class, "UserRegistration",
            new DummyUserRegistrationType()),
    CLIENT_REGISTRATION("client_registration", "Client Registration", ClientRegistrationType.class, CustomScript.class, "ClientRegistration",
            new DummyClientRegistrationType()),
    ID_GENERATOR("id_generator", "Id Generator", IdGeneratorType.class, CustomScript.class, "IdGenerator",
            new DummyIdGeneratorType()),
    UMA_RPT_POLICY("uma_rpt_policy", "UMA RPT Policies", UmaRptPolicyType.class, CustomScript.class, "UmaRptPolicy",
            new UmaDummyRptPolicyType()),
    UMA_RPT_CLAIMS("uma_rpt_claims", "UMA RPT Claims", UmaRptClaimsType.class, CustomScript.class, "UmaRptClaims", new UmaDummyRptClaimsType()),
    UMA_CLAIMS_GATHERING("uma_claims_gathering", "UMA Claims Gathering", UmaClaimsGatheringType.class, CustomScript.class, "UmaClaimsGathering",
            new UmaDummyClaimsGatheringType()),
    CONSENT_GATHERING("consent_gathering", "Consent Gathering", ConsentGatheringType.class, CustomScript.class, "ConsentGathering",
            new DummyConsentGatheringType()),
    DYNAMIC_SCOPE("dynamic_scope", "Dynamic Scopes", DynamicScopeType.class, CustomScript.class, "DynamicScope",
            new DummyDynamicScopeType()),
    SPONTANEOUS_SCOPE("spontaneous_scope", "Spontaneous Scopes", SpontaneousScopeType.class, CustomScript.class, "SpontaneousScope", new DummySpontaneousScopeType()),
    END_SESSION("end_session", "End Session", EndSessionType.class, CustomScript.class, "EndSession", new DummyEndSessionType()),
    POST_AUTHN("post_authn", "Post Authentication", PostAuthnType.class, CustomScript.class, "PostAuthn", new DummyPostAuthnType()),
    SCIM("scim", "SCIM", ScimType.class, CustomScript.class, "ScimEventHandler", new DummyScimType()),
    CIBA_END_USER_NOTIFICATION("ciba_end_user_notification", "CIBA End User Notification", EndUserNotificationType.class,
            CustomScript.class, "EndUserNotification", new DummyEndUserNotificationType()),
    REVOKE_TOKEN("revoke_token", "Revoke Token", RevokeTokenType.class, CustomScript.class, "RevokeToken", new DummyRevokeTokenType()),
    PERSISTENCE_EXTENSION("persistence_extension", "Persistence Extension", PersistenceType.class, CustomScript.class, "PersistenceExtension", new DummyPeristenceType()),
    IDP("idp", "Idp Extension", IdpType.class, CustomScript.class, "IdpExtension", new DummyIdpType()),
    UPDATE_TOKEN("update_token", "Update Token", UpdateTokenType.class, CustomScript.class, "UpdateToken", new DummyUpdateTokenType()),
    FIDO2_EXTENSION("fido2_extension", "Fido2 Extension", Fido2ExtensionType.class, CustomScript.class, "Fido2Extension", new DummyFido2ExtensionType());

    private String value;
    private String displayName;
    private Class<? extends BaseExternalType> customScriptType;
    private Class<? extends CustomScript> customScriptModel;
    private String pythonClass;
    private BaseExternalType defaultImplementation;

    private static Map<String, CustomScriptType> MAP_BY_VALUES = new HashMap<String, CustomScriptType>();

    static {
        for (CustomScriptType enumType : values()) {
            MAP_BY_VALUES.put(enumType.getValue(), enumType);
        }
    }

    CustomScriptType(String value, String displayName, Class<? extends BaseExternalType> customScriptType,
            Class<? extends CustomScript> customScriptModel, String pythonClass, BaseExternalType defaultImplementation) {
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
        return MAP_BY_VALUES.get(value);
    }

    public Enum<? extends AttributeEnum> resolveByValue(String value) {
        return getByValue(value);
    }

    @Override
    public String toString() {
        return value;
    }

}
