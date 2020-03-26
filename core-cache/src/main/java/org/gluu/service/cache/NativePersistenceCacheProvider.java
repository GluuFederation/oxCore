package org.gluu.service.cache;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.model.base.SimpleBranch;
import org.gluu.search.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Calendar;
import java.util.Date;

@ApplicationScoped
public class NativePersistenceCacheProvider extends AbstractCacheProvider<PersistenceEntryManager> {

    @Inject
    private Logger log;

    @Inject
    private CacheConfiguration cacheConfiguration;

    @Inject
    private PersistenceEntryManager entryManager;

    private String baseDn;

	private boolean deleteExpiredOnGetRequest;

    @PostConstruct
    public void init() {
    }

    @Override
    public void create() {
        try {
            baseDn = cacheConfiguration.getNativePersistenceConfiguration().getBaseDn();
            deleteExpiredOnGetRequest = cacheConfiguration.getNativePersistenceConfiguration().isDeleteExpiredOnGetRequest();

            if (StringUtils.isBlank(baseDn)) {
                log.error("Failed to create NATIVE_PERSISTENCE cache provider. 'baseDn' in CacheConfiguration is not initialized. It has to be set by client application (e.g. oxAuth has to set it in ApplicationFactory.)");
                throw new RuntimeException("Failed to create NATIVE_PERSISTENCE cache provider. 'baseDn' in CacheConfiguration is not initialized. It has to be set by client application.");
            }

            String branchDn = String.format("ou=cache,%s", baseDn);
            if (entryManager.hasBranchesSupport(branchDn)) {
	            if (!entryManager.contains(branchDn, SimpleBranch.class)) {
	                SimpleBranch branch = new SimpleBranch();
	                branch.setOrganizationalUnitName("cache");
	                branch.setDn(branchDn);
	
	                entryManager.persist(branch);
	            }
            }

            baseDn = branchDn;
            cacheConfiguration.getNativePersistenceConfiguration().setBaseDn(baseDn);

            log.info("Created NATIVE_PERSISTENCE cache provider. `baseDn`: " + baseDn);
        } catch (Exception e) {
            log.error("Failed to create NATIVE_PERSISTENCE cache provider.", e);
            throw new RuntimeException("Failed to create NATIVE_PERSISTENCE cache provider.", e);
        }
    }

	public void configure(CacheConfiguration cacheConfiguration, PersistenceEntryManager entryManager) {
		this.log = LoggerFactory.getLogger(NativePersistenceCacheProvider.class);
		this.cacheConfiguration = cacheConfiguration;
		this.entryManager = entryManager;
	}

    @Override
    public void destroy() {
    }

    @Override
    public PersistenceEntryManager getDelegate() {
        return entryManager;
    }

	@Override
	public boolean hasKey(String key) {
        try {
            key = hashKey(key);
            boolean hasKey = entryManager.contains(createDn(key), NativePersistenceCacheEntity.class);
            
//            log.trace("Contains key in cache, key: " + key + ", dn: " + createDn(key)) + ", contains: " + hasKey);
            return hasKey;
        } catch (Exception e) {
            // ignore, we call cache first which is empty and then fill it in
            // log.trace("No entry with key: " + originalKey + ", message: " + e.getMessage() + ", hashedKey: " + key);
        }

        return false;
	}

    @Override
    public Object get(String key) {
        try {
            key = hashKey(key);
            NativePersistenceCacheEntity entity = entryManager.find(NativePersistenceCacheEntity.class, createDn(key));
            if (entity != null && entity.getData() != null) {
                if (isExpired(entity.getNewExpirationDate()) && entity.isDeletable()) {
                    log.trace("Cache entity exists but expired, return null, expirationDate:" + entity.getExpirationDate() + ", key: " + key);
                    if (deleteExpiredOnGetRequest) {
                    	remove(key);
                    }
                    return null;
                }
                Object o = fromString(entity.getData());
//                log.trace("Returned object from cache, key: " + key + ", dn: " + entity.getDn());
                return o;
            }
        } catch (Exception e) {
            // ignore, we call cache first which is empty and then fill it in
            // log.trace("No entry with key: " + originalKey + ", message: " + e.getMessage() + ", hashedKey: " + key);
        }
        return null;
    }

    private String createDn(String key) {
        return String.format("uuid=%s,%s", key, baseDn);
    }

    public static String hashKey(String key) {
        return DigestUtils.sha256Hex(key);
    }

    @Override
    public void put(int expirationInSeconds, String key, Object object) {
        Date creationDate = new Date();

        expirationInSeconds = expirationInSeconds > 0 ? expirationInSeconds : cacheConfiguration.getNativePersistenceConfiguration().getDefaultPutExpiration();


        putImpl(key, object, creationDate, expirationInSeconds);
    }

	private void putImpl(String key, Object object, Date creationDate, int expirationInSeconds) {
        Calendar expirationDate = Calendar.getInstance();
		expirationDate.setTime(creationDate);
		expirationDate.add(Calendar.SECOND, expirationInSeconds);

		
		String originalKey = key;

        try {
            key = hashKey(key);
	
			NativePersistenceCacheEntity entity = new NativePersistenceCacheEntity();
			entity.setTtl(expirationInSeconds);
			entity.setData(asString(object));
			entity.setId(key);
			entity.setDn(createDn(key));
			entity.setCreationDate(creationDate);
			entity.setNewExpirationDate(expirationDate.getTime());
			entity.setDeletable(true);
	
			silentlyRemoveEntityIfExists(entity.getDn());
			entryManager.persist(entity);
        } catch (Exception e) {
        	log.error("Failed to put entry, key: " + originalKey + ", hashedKey: " + key + ", message: " + e.getMessage(), e); // log as trace since it is perfectly valid that entry is removed by timer for example
        }
	}

    private boolean silentlyRemoveEntityIfExists(String dn) {
        try {
            if (entryManager.find(NativePersistenceCacheEntity.class, dn) != null) {
                entryManager.remove(dn);
                return true;
            }
        } catch (Exception e) {
            // ignore
        }
        return false;
    }

    private static boolean isExpired(Date expiredAt) {
        return expiredAt == null || expiredAt.before(new Date());
    }

    @Override
    public void remove(String key) {
        if (silentlyRemoveEntityIfExists(createDn(hashKey(key)))) {
            log.trace("Removed entity, key: " + key);
        }
    }

    @Override
    public void clear() {
        // TODO: Implement all specific application objects removal
    }

    private Object fromString(String s) {
        try {
            byte[] data = Base64.decodeBase64(s);
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
            try {
                Object o = ois.readObject();
                ois.close();
                return o;
            } finally {
                IOUtils.closeQuietly(ois);
            }
        } catch (Exception e) {
            log.error("Failed to deserizalize cache entity, data: " + s, e);
            return null;
        }
    }

    private String asString(Object o) {
        ObjectOutputStream oos = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(o);
            oos.close();
            return Base64.encodeBase64String(baos.toByteArray());
        } catch (Exception e) {
            log.error("Failed to serizalize cache entity to string, object: " + 0, e);
            return null;
        } finally {
            IOUtils.closeQuietly(oos);
        }
    }

    @Override
    public void cleanup(final Date now) {
    	NativePersistenceConfiguration nativePersistenceConfiguration = cacheConfiguration.getNativePersistenceConfiguration();
		if (!entryManager.hasExpirationSupport(nativePersistenceConfiguration.getBaseDn())) {
			cleanup(now, cacheConfiguration.getNativePersistenceConfiguration().getDefaultCleanupBatchSize());
		}
    }

    public void cleanup(final Date now, int batchSize) {
        log.debug("Start NATIVE_PERSISTENCE clean up");
        try {
            Filter filter = Filter.createANDFilter(
                    Filter.createEqualityFilter("del", true),
                    Filter.createLessOrEqualFilter("exp", entryManager.encodeTime(baseDn, now)));
            final int removedCount = entryManager.remove(baseDn, NativePersistenceCacheEntity.class, filter, batchSize);

            log.debug("End NATIVE_PERSISTENCE clean up, items removed: " + removedCount);
        } catch (Exception e) {
            log.error("Failed to perform clean up.", e);
        }

    }

    @Override
    public CacheProviderType getProviderType() {
        return CacheProviderType.NATIVE_PERSISTENCE;
    }

    /* required for tests */
    public void setEntryManager(PersistenceEntryManager entryManager) {
        this.entryManager = entryManager;
    }

    /* required for tests */
    public void setBaseDn(String baseDn) {
        this.baseDn = baseDn;
    }

    /* required for tests */
    public void setCacheConfiguration(CacheConfiguration cacheConfiguration) {
        this.cacheConfiguration = cacheConfiguration;
    }

}
