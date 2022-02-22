/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.model;

import java.io.Serializable;

import org.gluu.persist.model.base.Entry;
import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.annotation.CustomObjectClass;
import org.gluu.persist.annotation.DataEntry;

/**
 * Entry with display Name attribute
 *
 * @author Yuriy Movchan Date: 08/11/2010
 */
@DataEntry(sortBy = "displayName")
public class DisplayNameEntry extends Entry implements Serializable {

    private static final long serialVersionUID = 2536007777903091939L;

    public DisplayNameEntry() {
    }

    public DisplayNameEntry(String dn, String inum, String displayName) {
        super(dn);
        this.inum = inum;
        this.displayName = displayName;
    }

    @CustomObjectClass
    private String[] customObjectClasses;

    @AttributeName(ignoreDuringUpdate = true)
    private String inum;

    @AttributeName
    private String displayName;

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

    @Override
    public String toString() {
        return String.format("DisplayNameEntry [displayName=%s, inum=%s, toString()=%s]", displayName, inum, super.toString());
    }

    public String[] getCustomObjectClasses() {
        return customObjectClasses;
    }

    public void setCustomObjectClasses(String[] customObjectClasses) {
        this.customObjectClasses = customObjectClasses;
    }

}
