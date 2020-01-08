/*
 /*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */

package org.gluu.persist.couchbase.impl;

import java.util.HashMap;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.gluu.persist.PersistenceEntryManagerFactory;
import org.gluu.persist.couchbase.operation.impl.CouchbaseConnectionProvider;
import org.gluu.persist.couchbase.operation.impl.CouchbaseOperationsServiceImpl;
import org.gluu.persist.exception.operation.ConfigurationException;
import org.gluu.persist.service.BaseFactoryService;
import org.gluu.util.PropertiesHelper;
import org.gluu.util.StringHelper;
import org.gluu.util.init.Initializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;

/**
 * Couchbase Entry Manager Factory
 *
 * @author Yuriy Movchan Date: 05/31/2018
 */
@ApplicationScoped
public class CouchbaseEntryManagerFactory extends Initializable implements PersistenceEntryManagerFactory {

    private static final Logger LOG = LoggerFactory.getLogger(CouchbaseEntryManagerFactory.class);

    public static final String PERSISTENCE_TYPE = "couchbase";

    private DefaultCouchbaseEnvironment.Builder builder;
    private CouchbaseEnvironment couchbaseEnvironment;

	private Properties couchbaseConnectionProperties;

    @PostConstruct
    public void create() {
    	this.builder = DefaultCouchbaseEnvironment.builder().operationTracingEnabled(false);
    }

	@Override
	protected void initInternal() {
        // SSL settings
        boolean useSSL = Boolean.valueOf(couchbaseConnectionProperties.getProperty("ssl.trustStore.enable")).booleanValue();
        if (useSSL) {
            String sslTrustStoreFile = couchbaseConnectionProperties.getProperty("ssl.trustStore.file");
            String sslTrustStorePin = couchbaseConnectionProperties.getProperty("ssl.trustStore.pin");

            builder.sslEnabled(true).sslTruststoreFile(sslTrustStoreFile).sslTruststorePassword(sslTrustStorePin);
        } else {
        	builder.sslEnabled(false);
        }
        
        String connectTimeoutString = couchbaseConnectionProperties.getProperty("connection.connect-timeout");
        if (StringHelper.isNotEmpty(connectTimeoutString)) {
        	int connectTimeout = Integer.valueOf(connectTimeoutString);
        	builder.connectTimeout(connectTimeout);
        }

        String operationTracingEnabledString = couchbaseConnectionProperties.getProperty("connection.operation-tracing-enabled");
        if (StringHelper.isNotEmpty(operationTracingEnabledString)) {
        	boolean operationTracingEnabled = Boolean.valueOf(operationTracingEnabledString);
        	builder.operationTracingEnabled(operationTracingEnabled);
        }

        String mutationTokensEnabledString = couchbaseConnectionProperties.getProperty("connection.mutation-tokens-enabled");
        if (StringHelper.isNotEmpty(mutationTokensEnabledString)) {
        	boolean mutationTokensEnabled = Boolean.valueOf(mutationTokensEnabledString);
        	builder.mutationTokensEnabled(mutationTokensEnabled);
        }

        String computationPoolSizeString = couchbaseConnectionProperties.getProperty("connection.computation-pool-size");
        if (StringHelper.isNotEmpty(computationPoolSizeString)) {
        	int computationPoolSize = Integer.valueOf(computationPoolSizeString);
        	builder.computationPoolSize(computationPoolSize);
        }

        this.couchbaseEnvironment = builder.build();

        this.builder = null;
	}

    @Override
    public String getPersistenceType() {
        return PERSISTENCE_TYPE;
    }

    @Override
    public HashMap<String, String> getConfigurationFileNames() {
    	HashMap<String, String> confs = new HashMap<String, String>();
    	confs.put(PERSISTENCE_TYPE, "gluu-couchbase.properties");

    	return confs;
    }
    
    public CouchbaseEnvironment getCouchbaseEnvironment() {
    	return couchbaseEnvironment;
    }

    @Override
    public CouchbaseEntryManager createEntryManager(Properties conf) {
		Properties entryManagerConf = PropertiesHelper.filterProperties(conf, PERSISTENCE_TYPE);

		// Allow proper initialization
		if (this.couchbaseConnectionProperties == null) {
			this.couchbaseConnectionProperties = entryManagerConf;
		}

    	init();
    	
    	if (!isInitialized()) {
            throw new ConfigurationException("Failed to create Couchbase environment!");
    	}

    	CouchbaseConnectionProvider connectionProvider = new CouchbaseConnectionProvider(entryManagerConf, couchbaseEnvironment);
        connectionProvider.create();
        if (!connectionProvider.isCreated()) {
            throw new ConfigurationException(
                    String.format("Failed to create Couchbase connection pool! Result code: '%s'", connectionProvider.getCreationResultCode()));
        }
        LOG.debug("Created connectionProvider '{}' with code '{}'", connectionProvider, connectionProvider.getCreationResultCode());

        CouchbaseEntryManager couchbaseEntryManager = new CouchbaseEntryManager(new CouchbaseOperationsServiceImpl(entryManagerConf, connectionProvider));
        LOG.info("Created CouchbaseEntryManager: {}", couchbaseEntryManager.getOperationService());

        return couchbaseEntryManager;
    }

	@Override
	public void initStandalone(BaseFactoryService persistanceFactoryService) {
		this.builder = DefaultCouchbaseEnvironment.builder().mutationTokensEnabled(true).computationPoolSize(5);
	}


/*
    public static void main(String[] args) throws FileNotFoundException, IOException {
    	Properties prop = new Properties();
    	prop.load(new FileInputStream(new File("D:/Temp/gluu-couchbase.properties")));
    	
    	CouchbaseEntryManagerFactory cemf = new CouchbaseEntryManagerFactory();
    	cemf.create();
    	
    	CouchbaseEntryManager cem = cemf.createEntryManager(prop);
        
        System.out.println(cem.getOperationService().getConnectionProvider().isCreated());
        
	}
*/
}
