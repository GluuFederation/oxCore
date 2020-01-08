package org.gluu.persist.hybrid.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.PersistenceEntryManagerFactory;
import org.gluu.persist.exception.operation.ConfigurationException;
import org.gluu.persist.operation.PersistenceOperationService;
import org.gluu.persist.service.BaseFactoryService;
import org.gluu.util.PropertiesHelper;
import org.gluu.util.StringHelper;
import org.gluu.util.properties.FileConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hybrid Entry Manager Factory
 *
 * @author Yuriy Movchan Date: 05/13/2018
 */
@ApplicationScoped
public class HybridEntryManagerFactory implements PersistenceEntryManagerFactory {
	static {
		if (System.getProperty("gluu.base") != null) {
			BASE_DIR = System.getProperty("gluu.base");
		} else if ((System.getProperty("catalina.base") != null) && (System.getProperty("catalina.base.ignore") == null)) {
			BASE_DIR = System.getProperty("catalina.base");
		} else if (System.getProperty("catalina.home") != null) {
			BASE_DIR = System.getProperty("catalina.home");
		} else if (System.getProperty("jboss.home.dir") != null) {
			BASE_DIR = System.getProperty("jboss.home.dir");
		} else {
			BASE_DIR = null;
		}
	}

	public static final String BASE_DIR;
	public static final String DIR = BASE_DIR + File.separator + "conf" + File.separator;

    public static final String PERSISTENCE_TYPE = "hybrid";
    public static final String PROPERTIES_FILE = "gluu-hybrid.properties";

	private static final Logger LOG = LoggerFactory.getLogger(HybridEntryManagerFactory.class);
	
	@Inject
	private BaseFactoryService persistanceFactoryService;

	private String[] persistenceTypes;

	private Properties hybridMappingProperties;

    @Override
    public String getPersistenceType() {
        return PERSISTENCE_TYPE;
    }

    @Override
    public HashMap<String, String> getConfigurationFileNames() {
    	HashMap<String, String> confs = new HashMap<String, String>();
    	confs.put(PERSISTENCE_TYPE, PROPERTIES_FILE);

    	HashMap<String, String> allConfs = getAllConfigurationFileNames(PROPERTIES_FILE);
    	confs.putAll(allConfs);

    	return confs;
    }

    private HashMap<String, String> getAllConfigurationFileNames(String confFileName) {
    	HashMap<String, String> allConfs = new HashMap<String, String>();

		FileConfiguration fileConf = new FileConfiguration(DIR + confFileName);
		if (!fileConf.isLoaded()) {
			LOG.error("Unable to load configuration file '{}'", DIR + confFileName);
            throw new ConfigurationException(String.format("Unable to load configuration file: '%s'", fileConf));
		}

		String storagesList = fileConf.getString("storages", null);
		if (StringHelper.isEmpty(storagesList)) {
            throw new ConfigurationException("'storages' key not exists or value is empty!");
		}
		
		this.persistenceTypes = StringHelper.split(storagesList, ",");
		for (String persistenceType : persistenceTypes) {
			PersistenceEntryManagerFactory persistenceEntryManagerFactory = persistanceFactoryService.getPersistenceEntryManagerFactory(persistenceType);
			if (persistenceEntryManagerFactory == null) {
				throw new ConfigurationException(String.format("Unable to get Persistence Entry Manager Factory by type '%s'", persistenceType));
			}
			
	    	Map<String, String> confs = persistenceEntryManagerFactory.getConfigurationFileNames();
	    	allConfs.putAll(confs);
		}

		return allConfs;
	}

    @Override
    public HybridEntryManager createEntryManager(Properties conf) {
    	HashMap<String, Properties> сonnectionProperties = new HashMap<String, Properties>();

    	HashMap<String, PersistenceEntryManager> persistenceEntryManagers = new HashMap<String, PersistenceEntryManager>();
    	List<PersistenceOperationService> operationServices = new ArrayList<PersistenceOperationService>();

		for (String persistenceType : persistenceTypes) {
			PersistenceEntryManagerFactory persistenceEntryManagerFactory = persistanceFactoryService.getPersistenceEntryManagerFactory(persistenceType);
			if (persistenceEntryManagerFactory == null) {
				throw new ConfigurationException(String.format("Unable to get Persistence Entry Manager Factory by type '%s'", persistenceType));
			}

			Properties entryManagerConf = PropertiesHelper.findProperties(conf, persistenceType);
    		PersistenceEntryManager persistenceEntryManager = persistenceEntryManagerFactory.createEntryManager(entryManagerConf);
    		
    		persistenceEntryManagers.put(persistenceType, persistenceEntryManager);
    		operationServices.add(persistenceEntryManager.getOperationService());

    		сonnectionProperties.put(persistenceType, entryManagerConf);
    	}

		this.hybridMappingProperties = PropertiesHelper.filterProperties(conf, PERSISTENCE_TYPE);
		
		HybridPersistenceOperationService hybridOperationService = new HybridPersistenceOperationService(operationServices);
    	
        HybridEntryManager hybridEntryManager = new HybridEntryManager(hybridMappingProperties, persistenceEntryManagers, hybridOperationService);
        LOG.info("Created HybridEntryManager: {}", hybridOperationService);

        return hybridEntryManager;
    }

	@Override
	public void initStandalone(BaseFactoryService persistanceFactoryService) {
		this.persistanceFactoryService = persistanceFactoryService;
	}

}
