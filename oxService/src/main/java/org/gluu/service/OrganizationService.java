/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 *//**
     *
     */
package org.gluu.service;

import java.io.Serializable;

import org.gluu.model.ApplicationType;

/**
 * @author "Oleksiy Tataryn"
 *
 */
public abstract class OrganizationService implements Serializable {

    private static final long serialVersionUID = -6601700282123372943L;

    public static final int ONE_MINUTE_IN_SECONDS = 60;

    public String getDnForOrganization(String baseDn) {
        if (baseDn == null) {
            baseDn = "o=gluu";
        }
        return baseDn;
    }

    public abstract ApplicationType getApplicationType();
}
