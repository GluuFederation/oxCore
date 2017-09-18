/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.model;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

/**
 * Simple custom property to hold key/value/description
 *
 * @author Yuriy Movchan Date: 02/08/2011
 */
@XmlRootElement
@JsonPropertyOrder({ "value1", "value2", "description" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class SimpleCustomProperty implements Serializable {

	private static final long serialVersionUID = -1451889014702205980L;

	private String value1;
	private String value2;
	private String description;

    public SimpleCustomProperty() {
        this("", "");
    }

    public SimpleCustomProperty(String value1, String value2) {
		this(value1, value2, "");
	}

    public SimpleCustomProperty(String p_value1, String p_value2, String p_description) {
        description = p_description;
        value1 = p_value1;
        value2 = p_value2;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String p_description) {
        description = p_description;
    }

    public final String getValue1() {
		return value1;
	}

	public final void setValue1(String value1) {
		this.value1 = value1;
	}

	public String getValue2() {
		return value2;
	}

	public void setValue2(String value2) {
		this.value2 = value2;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((value1 == null) ? 0 : value1.hashCode());
		result = prime * result + ((value2 == null) ? 0 : value2.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SimpleCustomProperty other = (SimpleCustomProperty) obj;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (value1 == null) {
			if (other.value1 != null)
				return false;
		} else if (!value1.equals(other.value1))
			return false;
		if (value2 == null) {
			if (other.value2 != null)
				return false;
		} else if (!value2.equals(other.value2))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return String.format("SimpleCustomProperty [value1=%s, value2=%s, description=%s]", value1, value2, description);
	}

}
