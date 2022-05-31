package org.gluu.service.document.store.provider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Date;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.ldap.impl.LdapEntryManager;
import org.gluu.persist.ldap.impl.LdapEntryManagerFactory;
import org.gluu.service.document.store.conf.DBDocumentStoreConfiguration;
import org.gluu.service.document.store.conf.DocumentStoreConfiguration;
import org.gluu.service.document.store.conf.DocumentStoreType;
import org.gluu.service.document.store.service.DBDocumentService;
import org.gluu.service.document.store.service.OxDocument;
import org.gluu.util.StringHelper;
import org.gluu.util.security.StringEncrypter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Shekhar L. on 26/04/2022
 */
@ApplicationScoped
public class DBDocumentStoreProvider extends DocumentStoreProvider<DBDocumentStoreProvider> {

	@Inject
	private Logger log;
	

	@Inject
	private DBDocumentService documentService;
	
	@Inject
	private DocumentStoreConfiguration documentStoreConfiguration;

	private DBDocumentStoreConfiguration dbDocumentStoreConfiguration;
	
	
	@Inject
	private PersistenceEntryManager persistenceEntryManager;

    public DBDocumentStoreProvider() {
	}

	public DocumentStoreType getProviderType() {
		return DocumentStoreType.DB;
	}
	
	@PostConstruct
	public void init() {
		this.dbDocumentStoreConfiguration = documentStoreConfiguration.getDbConfiguration();
	}
	
	public void configure(DocumentStoreConfiguration documentStoreConfiguration,PersistenceEntryManager persistenceManager) {
		this.log = LoggerFactory.getLogger(DBDocumentStoreProvider.class);
		this.documentStoreConfiguration = documentStoreConfiguration;
		this.persistenceEntryManager = persistenceManager;
		
	}

	@Override
	public void create() {
		
	}

	@Override
	public void destroy() {
		
	}

	@Override
	public boolean hasDocument(String DisplayName) {
		log.debug("Has document: '{}'", DisplayName);
		if (StringHelper.isEmpty(DisplayName)) {
			throw new IllegalArgumentException("Specified path should not be empty!");
		}		
		OxDocument oxDocument = null;
		try {
			oxDocument = documentService.getOxDocumentByDisplayName(DisplayName);
		} catch (Exception e) {
			log.error("Failed to check if path '" + DisplayName + "' exists in repository", e);
		}

		return false;
	}

	@Override
	public boolean saveDocument(String name, String documentContent, Charset charset) {
		log.debug("Save document: '{}'", name);
		OxDocument oxDocument = new OxDocument();
		oxDocument.setDocument(documentContent);
		oxDocument.setDisplayName(name);		
		try {
			try {
				oxDocument.setInum(documentService.generateInumForNewOxDocument());	
				String dn = "inum="+ oxDocument.getInum() +",ou=document,o=gluu";
				oxDocument.setDn(dn);
				oxDocument.setDescription("Testing the document saving");
				oxDocument.setOxEnabled("true");
				oxDocument.setOxModuleProperty("oxtrusr server");
				documentService.addOxDocument(oxDocument);
				//persistenceEntryManager.persist(oxDocument);
				return true;
			} finally {
			}
		} catch (Exception ex) {
			log.error("Failed to write document to file '{}'", name, ex);
		}

		return false;
	}

	@Override
	public boolean saveDocumentStream(String name, InputStream documentStream) {
		
		//log.debug("Save document from stream: '{}'", name);
		OxDocument oxDocument = new OxDocument();
		oxDocument.setDisplayName(name);
		
		 try {
			String documentContent = Base64.getEncoder().encodeToString(IOUtils.toByteArray(documentStream));
			oxDocument.setDocument(documentContent);
			String inum = documentService.generateInumForNewOxDocument();
			oxDocument.setInum(inum);	
			String dn = "inum="+ oxDocument.getInum() +",ou=document,o=gluu";
			oxDocument.setDn(dn);
			oxDocument.setDisplayName("Test");
			oxDocument.setDescription("Testing the document saving");
			oxDocument.setOxEnabled("true");
			oxDocument.setOxModuleProperty("oxtrusr server");
			documentService.addOxDocument(oxDocument);
			//persistenceEntryManager.persist(oxDocument);
			return true;
		} catch (IOException e) {
			log.error("Failed to write document from stream to file '{}'", name, e);
		}catch (Exception e) {
			log.error("Failed to write document from stream to file '{}'", name, e);
		}	

		return false;
	}


	@Override
	public String readDocument(String name, Charset charset) {
		log.debug("Read document: '{}'", name);		
		OxDocument oxDocument;
		try {
			oxDocument = documentService.getOxDocumentByDisplayName(name);
			if(oxDocument != null) {
				return oxDocument.getDocument();
			}
		} catch (Exception e) {
			log.error("Failed to read document as stream from file '{}'", name, e);
		}
		return null;		
	}

	@Override
	public InputStream readDocumentAsStream(String name) {
		log.debug("Read document as stream: '{}'", name);
		String filecontecnt = readDocument(name, null);
		if (filecontecnt == null) {
			log.error("Document file '{}' isn't exist", name);
			return null;
		}

		InputStream InputStream = new ByteArrayInputStream(Base64.getDecoder().decode(filecontecnt));
		return InputStream;
	}

	@Override
	public boolean renameDocument(String currentDisplayName, String destinationDisplayName) {
		log.debug("Rename document: '{}' -> '{}'", currentDisplayName, destinationDisplayName);
		OxDocument oxDocument;
		try {
			oxDocument = documentService.getOxDocumentByDisplayName(currentDisplayName);
			if (oxDocument == null) {
				log.error("Document doesn't Exist with the name  '{}'", currentDisplayName);
				return false;
			}
			oxDocument.setDisplayName(destinationDisplayName);
			documentService.updateOxDocument(oxDocument);
			OxDocument oxDocumentDestination = documentService.getOxDocumentByDisplayName(destinationDisplayName);
			if(oxDocumentDestination == null) {
				log.error("Failed to rename to destination file '{}'", destinationDisplayName);
				return false;
			}
		} catch (Exception e) {
			log.error("Failed to rename to destination file '{}'", destinationDisplayName);
		}
		return true;
	}

	@Override
	public boolean removeDocument(String inum) {
		log.debug("Remove document: '{}'", inum);
		OxDocument oxDocument;
		try {
			oxDocument = documentService.getOxDocumentByInum(inum);
			if(oxDocument == null) {
				log.error(" document not exist file '{}'", inum);
				return false;
			}
			
			documentService.removeOxDocument(oxDocument);
			OxDocument checkOxDocument = documentService.getOxDocumentByInum(inum);
			if(checkOxDocument == null) {
				return true;
			}
			return false;
		} catch (Exception e) {
			log.error("Failed to remove document file '{}'", inum, e);
		}
		return false;
	}

}
