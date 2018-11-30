package org.xdi.service.ldap;

import org.xdi.service.ldap.LdapConnectionService;

public class LdapConnectionProviders {
    private LdapConnectionService connectionProvider;
    private LdapConnectionService connectionBindProvider;

    public LdapConnectionProviders(LdapConnectionService connectionProvider, LdapConnectionService connectionBindProvider) {
        this.connectionProvider = connectionProvider;
        this.connectionBindProvider = connectionBindProvider;
    }

    public LdapConnectionService getConnectionProvider() {
        return connectionProvider;
    }

    public LdapConnectionService getConnectionBindProvider() {
        return connectionBindProvider;
    }
}
