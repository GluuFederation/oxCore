package org.xdi.config.oxtrust;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import javax.enterprise.inject.Vetoed;
import java.io.Serializable;
import java.util.List;

/**
 * oxTrust configuration
 * 
 * @author Yuriy Movchan Date: 09/04/2017
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Vetoed
public class ImportPersonConfig implements Configuration, Serializable {
	
	private static final long serialVersionUID = 2686538577505167695L;

	private List <ImportPerson> mappings;

	public List<ImportPerson> getMappings() {
		return mappings;
	}

	public void setMappings(List<ImportPerson> mappings) {
		this.mappings = mappings;
	}

}
