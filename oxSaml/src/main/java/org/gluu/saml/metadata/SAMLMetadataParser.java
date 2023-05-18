/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */
package org.gluu.saml.metadata;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.gluu.service.document.store.service.DocumentStoreService;
import org.slf4j.Logger;
import org.xml.sax.SAXException;

/**
 * SAML metadata parser.
 *
 * @author Dmitry Ognyannikov
 */

@ApplicationScoped
public class SAMLMetadataParser {

	@Inject
	private Logger log;

	@Inject
	private DocumentStoreService documentStoreService;

    public List<String> getEntityIdFromMetadataFile(String metadataFile) {
        if (!documentStoreService.hasDocument(metadataFile)) {
            return null;
        }

        EntityIDHandler handler = parseMetadata(metadataFile);
        if(handler!=null){
            List<String> entityIds = handler.getEntityIDs();
            if (entityIds == null || entityIds.isEmpty()) {
                log.error("Failed to find entityId in metadata file: " + metadataFile);
            }
            return entityIds;
        }else{
           return null;
        }
    }
    
    public List<String> getEntityIdFromMetadataFile(File metadataFile) {
        if (!metadataFile.isFile()) {
            return null;
        }
        EntityIDHandler handler = parseMetadata(metadataFile);

        List<String> entityIds = handler.getEntityIDs();

        if (entityIds == null || entityIds.isEmpty()) {
        	log.error("Failed to find entityId in metadata file: " + metadataFile.getAbsolutePath());
        }

        return entityIds;
    }

    public List<String> getSpEntityIdFromMetadataFile(String metadataFile) {
        EntityIDHandler handler = parseMetadata(metadataFile);
        if(handler!=null){
            List<String> entityIds = handler.getSpEntityIDs();

            if (entityIds == null || entityIds.isEmpty()) {
            	log.error("Failed to find entityId in metadata file: " + metadataFile);
            }

            return entityIds;
        }else {
            return null;
        }

    }

    public EntityIDHandler parseMetadata(String metadataFile) {
        if (!documentStoreService.hasDocument(metadataFile)) {
            log.error("Failed to get entityId from metadata file: " + metadataFile);
            return null;
        }

        InputStream is = null;
        try {
            is = documentStoreService.readDocumentAsStream(metadataFile);

            return parseMetadata(is);
        } catch (Exception ex) {
            log.error("Failed to read SAML metadata file: " + metadataFile, ex);
            return null;
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
    
    public EntityIDHandler parseMetadata(File metadataFile) {
        if (!metadataFile.exists()) {
            log.error("Failed to get entityId from metadata file: " + metadataFile.getAbsolutePath());
            return null;
        }

        InputStream is = null;
        try {
            is = FileUtils.openInputStream(metadataFile);
        
            return parseMetadata(is);
        } catch (IOException ex) {
            log.error("Failed to read SAML metadata file: " + metadataFile.getAbsolutePath(), ex);
            return null;
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public EntityIDHandler parseMetadata(InputStream is) {
        InputStreamReader isr = null;
        EntityIDHandler handler = null;
        try {
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            SAXParser saxParser = saxParserFactory.newSAXParser();
            handler = new EntityIDHandler();
            saxParser.parse(is, handler);
        } catch (IOException ex) {
            log.error("Failed to read SAML metadata", ex);
        } catch (ParserConfigurationException e) {
            log.error("Failed to confugure SAX parser", e);
        } catch (SAXException e) {
            log.error("Failed to parse SAML metadata", e);
        } finally {
            IOUtils.closeQuietly(isr);
            IOUtils.closeQuietly(is);
        }


        return handler;
    }

    public EntityIDHandler parseMetadata(URL metadataURL) throws ClientProtocolException, IOException {
		byte[] metadataFileContent = downloadMetadata(metadataURL.toExternalForm());

        InputStream is = new ByteArrayInputStream(metadataFileContent);

        return parseMetadata(is);
    }

    public byte[] downloadMetadata(String metadataURL) throws IOException, ClientProtocolException {
		HttpGet httpGet = new HttpGet(metadataURL);
    	httpGet.setHeader("Accept", "application/xml, text/xml");

    	byte[] metadataFileContent = null;
    	try ( CloseableHttpClient httpClient = HttpClients.custom()
				.setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
				.build() ) {
    		HttpResponse httpResponse = httpClient.execute(httpGet);
    		metadataFileContent = getResponseContent(httpResponse);
		}

        if (metadataFileContent == null) {
            return null;
        }

        return metadataFileContent;
	}

    public byte[] getResponseContent(HttpResponse httpResponse) throws IOException {
        if ((httpResponse == null) || (httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK)) {
        	return null;
        }

        HttpEntity entity = httpResponse.getEntity();
		byte[] responseBytes = new byte[0];
		if (entity != null) {
			responseBytes = EntityUtils.toByteArray(entity);
		}

    	// Consume response content
		if (entity != null) {
			EntityUtils.consume(entity);
		}

    	return responseBytes;
	}

}

