package org.gluu.service.document.store.provider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Base64;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.gluu.service.document.store.conf.DocumentStoreType;
import org.gluu.service.document.store.service.DBDocumentService;
import org.gluu.service.document.store.service.OxDocument;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;

/**
 * @author Shekhar L. on 26/04/2022
 */
@ApplicationScoped
public class DBDocumentStoreProvider extends DocumentStoreProvider<DBDocumentStoreProvider> {

	@Inject
	private Logger log;
	

	@Inject
	private DBDocumentService documentService;

    public DBDocumentStoreProvider() {
	}

	public DocumentStoreType getProviderType() {
		return DocumentStoreType.DB;
	}

	@Override
	public boolean hasDocument(String path) {
		log.debug("Has document: '{}'", path);
		if (StringHelper.isEmpty(path)) {
			throw new IllegalArgumentException("Specified path should not be empty!");
		}		
		OxDocument oxDocument = null;
		try {
			oxDocument = documentService.getOxDocumentByDisplayName(path);
		} catch (Exception e) {
			log.error("Failed to check if path '" + path + "' exists in repository", e);
		}

		return oxDocument != null;
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
			oxDocument.setInum(documentService.generateInumForNewOxDocument());				
			documentService.addOxDocument(oxDocument);
			return true;
		} catch (IOException e) {
			log.error("Failed to write document from stream to file '{}'", name, e);
		}catch (Exception e) {
			log.error("Failed to write document from stream to file '{}'", name, e);
		}	

		return false;
	}


	@Override
	public String readDocument(String inum, Charset charset) {
		log.debug("Read document: '{}'", inum);		
		OxDocument oxDocument;
		try {
			oxDocument = documentService.getOxDocumentByInum(inum);
			if(oxDocument != null) {
				return oxDocument.getDocument();
			}
		} catch (Exception e) {
			log.error("Failed to read document as stream from file '{}'", inum, e);
			e.printStackTrace();
		}
		return null;		
	}

	@Override
	public InputStream readDocumentAsStream(String inum) {
		log.debug("Read document as stream: '{}'", inum);
		String filecontecnt = readDocument(inum, null);
		if (filecontecnt == null) {
			log.error("Document file '{}' isn't exist", inum);
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
			e.printStackTrace();
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
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public void create() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

}
