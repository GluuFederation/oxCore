package org.gluu.service.cache;

import com.google.common.collect.Maps;
import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.annotation.DN;
import org.gluu.persist.annotation.JsonObject;

import javax.annotation.Nonnull;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 */
public class SampleSessionId implements Serializable {

    private static final long serialVersionUID = -137476411915686378L;

    @DN
    private String dn;

    @AttributeName(name = "oxAuthSessionId")
    private String id;

    @AttributeName(name = "oxLastAccessTime")
    private Date lastUsedAt;

    @AttributeName(name = "oxAuthUserDN")
    private String userDn;

    @AttributeName(name = "authnTime")
    private Date authenticationTime;

    @AttributeName(name = "oxState")
    private SessionIdState state;

    @AttributeName(name = "oxAuthSessionState")
    private String sessionState;

    @AttributeName(name = "oxAuthPermissionGranted")
    private Boolean permissionGranted;

    @AttributeName(name = "oxAsJwt")
    private Boolean isJwt = false;

    @AttributeName(name = "oxJwt")
    private String jwt;

    @JsonObject
    @AttributeName(name = "oxAuthPermissionGrantedMap")
    private SessionIdAccessMap permissionGrantedMap;

    @JsonObject
    @AttributeName(name = "oxInvolvedClients")
    private SessionIdAccessMap involvedClients;

    @JsonObject
    @AttributeName(name = "oxAuthSessionAttribute")
    private Map<String, String> sessionAttributes;

    @Transient
    private transient boolean persisted;

    public SampleSessionId() {
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String p_dn) {
        dn = p_dn;
    }

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }

    public Boolean getIsJwt() {
        return isJwt;
    }

    public void setIsJwt(Boolean isJwt) {
        this.isJwt = isJwt;
    }

    public SessionIdAccessMap getInvolvedClients() {
        if (involvedClients == null) {
            involvedClients = new SessionIdAccessMap();
        }
        return involvedClients;
    }

    public void setInvolvedClients(SessionIdAccessMap involvedClients) {
        this.involvedClients = involvedClients;
    }

    public SessionIdState getState() {
        return state;
    }

    public void setState(SessionIdState state) {
        this.state = state;
    }

    public String getSessionState() {
        return sessionState;
    }

    public void setSessionState(String sessionState) {
        this.sessionState = sessionState;
    }

    public String getOPBrowserState() {
        return sessionAttributes.get("opbs");
    }

    public String getId() {
        return id;
    }

    public void setId(String p_id) {
        id = p_id;
    }

    public Date getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Date p_lastUsedAt) {
        lastUsedAt = p_lastUsedAt;
    }

    public String getUserDn() {
        return userDn;
    }

    public void setUserDn(String p_userDn) {
        userDn = p_userDn != null ? p_userDn : "";
    }

    public Date getAuthenticationTime() {
        return authenticationTime;
    }

    public void setAuthenticationTime(Date authenticationTime) {
        this.authenticationTime = authenticationTime;
    }

    public Boolean getPermissionGranted() {
        return permissionGranted;
    }

    public void setPermissionGranted(Boolean permissionGranted) {
        this.permissionGranted = permissionGranted;
    }

    public SessionIdAccessMap getPermissionGrantedMap() {
        return permissionGrantedMap;
    }

    public void setPermissionGrantedMap(SessionIdAccessMap permissionGrantedMap) {
        this.permissionGrantedMap = permissionGrantedMap;
    }

    public Boolean isPermissionGrantedForClient(String clientId) {
        return permissionGrantedMap != null && permissionGrantedMap.get(clientId);
    }

    public void addPermission(String clientId, Boolean granted) {
        if (permissionGrantedMap == null) {
            permissionGrantedMap = new SessionIdAccessMap();
        }
        permissionGrantedMap.put(clientId, granted);
    }

    @Nonnull
    public Map<String, String> getSessionAttributes() {
        if (sessionAttributes == null) {
            sessionAttributes = Maps.newHashMap();
        }
        return sessionAttributes;
    }

    public void setSessionAttributes(Map<String, String> sessionAttributes) {
        this.sessionAttributes = sessionAttributes;
    }

    public boolean isPersisted() {
        return persisted;
    }

    public void setPersisted(boolean persisted) {
        this.persisted = persisted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SampleSessionId id1 = (SampleSessionId) o;

        return !(id != null ? !id.equals(id1.id) : id1.id != null);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("SessionState {");
        sb.append("dn='").append(dn).append('\'');
        sb.append(", id='").append(id).append('\'');
        sb.append(", lastUsedAt=").append(lastUsedAt);
        sb.append(", userDn='").append(userDn).append('\'');
        sb.append(", authenticationTime=").append(authenticationTime);
        sb.append(", state=").append(state);
        sb.append(", sessionState='").append(sessionState).append('\'');
        sb.append(", permissionGranted=").append(permissionGranted);
        sb.append(", isJwt=").append(isJwt);
        sb.append(", jwt=").append(jwt);
        sb.append(", permissionGrantedMap=").append(permissionGrantedMap);
        sb.append(", involvedClients=").append(involvedClients);
        sb.append(", sessionAttributes=").append(sessionAttributes);
        sb.append(", persisted=").append(persisted);
        sb.append("}");
        return sb.toString();
    }
}
