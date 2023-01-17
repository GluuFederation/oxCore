package org.gluu.config.oxtrust;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.enterprise.inject.Vetoed;

import org.apache.commons.lang3.builder.DiffBuilder;
import org.apache.commons.lang3.builder.DiffResult;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.util.List;


/**
 * oxTrust configuration
 *
 * @author shekhar laad
 * @date 12/10/2015
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Vetoed
public class ImportPersonConfig implements Configuration, Serializable {

    private static final long serialVersionUID = 2686538577505167695L;

    private List<ImportPerson> mappings;

    public List<ImportPerson> getMappings() {
        return mappings;
    }

    public void setMappings(List<ImportPerson> mappings) {
        this.mappings = mappings;
    }

	@Override
	public DiffResult diff(Configuration newObj) {
		ImportPersonConfig obj = (ImportPersonConfig) newObj;
		 return new DiffBuilder(this, obj, ToStringStyle.SHORT_PREFIX_STYLE)
			        .append("mappings", this.mappings, obj.mappings)
			        .build();
	}

}
