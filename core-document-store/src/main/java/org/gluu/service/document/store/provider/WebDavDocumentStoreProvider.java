package org.gluu.service.document.store.provider;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.SchemePortResolver;
import org.apache.http.conn.UnsupportedSchemeException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Args;
import org.apache.jackrabbit.webdav.client.methods.HttpMkcol;
import org.gluu.service.document.store.conf.DocumentStoreConfiguration;
import org.gluu.service.document.store.conf.DocumentStoreType;
import org.gluu.service.document.store.conf.WebDavDocumentStoreConfiguration;
import org.gluu.util.security.StringEncrypter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;

/**
 * @author Yuriy Movchan on 04/10/2020
 */
@ApplicationScoped
public class WebDavDocumentStoreProvider extends DocumentStoreProvider<WebDavDocumentStoreProvider> {

	@Inject
    private Logger log;

    @Inject
    private DocumentStoreConfiguration documentStoreConfiguration;

    @Inject
    private StringEncrypter stringEncrypter;

    /**
	 * Local context with authentication cache. Make sure the same context is used to execute
	 * logically related requests.
	 */
	protected HttpClientContext context = HttpClientContext.create();

    private WebDavDocumentStoreConfiguration webDavDocumentStoreConfiguration;
    
    private Sardine client;
    private HttpClient httpClient;
	private String baseServerUrl;
	private int connectionTimeout;

    public WebDavDocumentStoreProvider() {
    }

    @PostConstruct
    public void init() {
        this.webDavDocumentStoreConfiguration = documentStoreConfiguration.getWebDavConfiguration();
    }

	public void create() {
		try {
			log.debug("Starting WebDavDocumentStoreProvider ...");
			decryptPassword(webDavDocumentStoreConfiguration);

			String password = StringUtils.isBlank(webDavDocumentStoreConfiguration.getDecryptedPassword()) ? "" : webDavDocumentStoreConfiguration.getDecryptedPassword();

			UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(
					webDavDocumentStoreConfiguration.getUserId(), password);

			this.connectionTimeout = webDavDocumentStoreConfiguration.getConnectionTimeout();

			this.httpClient = createHttpClient(credentials, connectionTimeout * 1000);

	        this.baseServerUrl = webDavDocumentStoreConfiguration.getServerUrl() + "/" +
					webDavDocumentStoreConfiguration.getWorkspaceName();
/*
 * Sardine
*/			

			this.client = SardineFactory.begin(webDavDocumentStoreConfiguration.getUserId(), password);
	    } catch (Exception ex) {
	        throw new IllegalStateException("Error starting JcaDocumentStoreProvider", ex);
	    }
	}

	public void configure(DocumentStoreConfiguration documentStoreConfiguration, StringEncrypter stringEncrypter) {
		this.log = LoggerFactory.getLogger(WebDavDocumentStoreProvider.class);
		this.documentStoreConfiguration = documentStoreConfiguration;
		this.stringEncrypter = stringEncrypter;
	}

    @PreDestroy
    public void destroy() {
    	log.debug("Destroying WebDavDocumentStoreProvider");

    	this.client = null;

        log.debug("Destroyed WebDavDocumentStoreProvider");
    }

    @Override
    public DocumentStoreType getProviderType() {
        return DocumentStoreType.WEB_DAV;
    }

	@Override
	public boolean hasDocument(String path) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean saveDocument(String path, String documentContent, Charset charset, List <String> moduleList) {
		if (true) {
		log.debug("Save document: '{}'", path);
		
		String normalizedPath = getNormalizedPath(path);
		try {
			HttpPut method = new HttpPut(baseServerUrl + "/" + normalizedPath);
			HttpEntity entity = new StringEntity(documentContent, charset);
			method.setEntity(entity);

			HttpContext requestLocalContext = new BasicHttpContext(context);
			HttpResponse response = httpClient.execute(method, requestLocalContext);
			
			int statusCode = response.getStatusLine().getStatusCode();
			return statusCode == HttpStatus.SC_CREATED || statusCode == HttpStatus.SC_NO_CONTENT;
		} catch (IOException ex) {
			log.error("Failed to write document to file '{}'", path, ex);
		}

		return false;
		} else {

		log.debug("Save document: '{}'", path);
		
		String normalizedPath = getNormalizedPath(path);
		try {
			String url = baseServerUrl + "/" + normalizedPath;
			client.put(url, IOUtils.toInputStream(documentContent));
			
			return true;
		} catch (IOException ex) {
			log.error("Failed to write document to file '{}'", path, ex);
		}

		return false;
		}
	}

	@Override
	public boolean saveDocumentStream(String path, InputStream documentStream, List <String> moduleList) {
		log.debug("Save document from stream: '{}'", path);
		String normalizedPath = getNormalizedPath(path);
		try {
			HttpPut method = new HttpPut(baseServerUrl + "/" + normalizedPath);
			HttpEntity entity = new InputStreamEntity(documentStream);
			method.setEntity(entity);

			HttpContext requestLocalContext = new BasicHttpContext(context);
			HttpResponse response = httpClient.execute(method, requestLocalContext);
			
			return response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED;
		} catch (IOException ex) {
			log.error("Failed to write document from stream to file '{}'", path, ex);
		}

		return false;
	}

	@Override
	public String readDocument(String path, Charset charset) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream readDocumentAsStream(String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean renameDocument(String currentPath, String destinationPath) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean removeDocument(String path) {
		// TODO Auto-generated method stub
		return false;
	}

	private void createPath(String path) throws RepositoryException {
/*
		HttpMkcol mkcol = new HttpMkcol("");
		this.client.executeMethod(mkcol, )
		
		File filePath = new File(path);
		String folderPath = filePath.getParentFile().getPath();

		String normalizedFolderPath = getNormalizedPath(folderPath);
		JcrUtils.getOrCreateByPath(normalizedFolderPath, NodeType.NT_FOLDER, session);
*/
	}
    private HttpClient createHttpClient(UsernamePasswordCredentials credentials, int timeoutInMillis) {
    	CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
//    	credentialsProvider.setCredentials(AuthScope.ANY, credentials);
    	credentialsProvider.setCredentials(
				new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM, AuthSchemes.BASIC),
				credentials);
    	
    	SchemePortResolver portResolver = new DefaultSchemePortResolver() {
    	    @Override
			public int resolve(final HttpHost host) throws UnsupportedSchemeException {
				Args.notNull(host, "HTTP host");
				final int port = host.getPort();
				if (port > 0) {
					return port;
				}
				final String name = host.getSchemeName();
				if (name.equalsIgnoreCase("http")) {
					return 8080;
				} else if (name.equalsIgnoreCase("https")) {
					return 8443;
				}

				return super.resolve(host);
			}
    	};
    	
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        CloseableHttpClient httpClient = HttpClients.custom()
				.setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD)
				.setSocketTimeout(timeoutInMillis).setExpectContinueEnabled(false).build())
				.setDefaultCredentialsProvider(credentialsProvider)
        		.setConnectionManager(cm)
/*				.setRetryHandler(new DefaultHttpRequestRetryHandler(0, false))*/
/*				.setSchemePortResolver(portResolver)*/
        		
        		.build();
        cm.setMaxTotal(50); // Increase max total connection to 50
        cm.setDefaultMaxPerRoute(10); // Increase default max connection per route to 10
        
        context.setCredentialsProvider(credentialsProvider);
        
        return httpClient;
    }

    private void decryptPassword(WebDavDocumentStoreConfiguration webDocumentStoreConfiguration) {
        try {
            String encryptedPassword = webDocumentStoreConfiguration.getPassword();
            if (StringUtils.isNotBlank(encryptedPassword)) {
            	webDocumentStoreConfiguration.setDecryptedPassword(stringEncrypter.decrypt(encryptedPassword));
                log.trace("Decrypted WebDAV password successfully.");
            }
        } catch (StringEncrypter.EncryptionException ex) {
            log.error("Error during WebDAV password decryption", ex);
        }
    }

	private String getNormalizedPath(String path) {
		String resultPath = path.replace("\\",  "/").replace(" ", "");
		if (resultPath.startsWith("/")) {
			resultPath = resultPath.substring(1);
		}
		
		return resultPath;
	}

}
