/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.config.oxtrust;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.enterprise.inject.Vetoed;

import org.apache.commons.lang3.builder.DiffBuilder;
import org.apache.commons.lang3.builder.DiffResult;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;


/**
 * Attribute resolver configurations
 *
 * @author Yuriy Movchan Date: 09/04/2017
 */
@Vetoed
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttributeResolverConfiguration implements Configuration {

    private List<NameIdConfig> nameIdConfigs;

    public List<NameIdConfig> getNameIdConfigs() {
        return nameIdConfigs;
    }

    public void setNameIdConfigs(List<NameIdConfig> nameIdConfigs) {
        this.nameIdConfigs = nameIdConfigs;
    }

	@Override
	public DiffResult diff(Configuration newObj) {
		AttributeResolverConfiguration obj = (AttributeResolverConfiguration) newObj;
		 return new DiffBuilder(this, obj, ToStringStyle.SHORT_PREFIX_STYLE)
			        .append("nameIdConfigs", this.nameIdConfigs, obj.nameIdConfigs)
			        .build();
	}
	
	

}
