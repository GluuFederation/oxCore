package org.gluu.service.document.store.conf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

import org.gluu.persist.PersistenceEntryManager;

/**
 * @author shekhar L. on 27/04/2022
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DBDocumentStoreConfiguration implements Serializable {

	private static final long serialVersionUID = 3380170170265842538L;

	private PersistenceEntryManager persistenceEntryManager = null;

	public PersistenceEntryManager getPersistenceEntryManager() {
		return persistenceEntryManager;
	}

	public void setPersistenceEntryManager(PersistenceEntryManager persistenceEntryManager) {
		this.persistenceEntryManager = persistenceEntryManager;
	}

	@Override
	public String toString() {
		return "DBDocumentStoreConfiguration [persistenceEntryManager=" + persistenceEntryManager + "]";
	}

}
