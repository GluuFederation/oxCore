package org.xdi.service.cache;

import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

/**
 * @author yuriyz on 02/23/2017.
 */
public class RedisProvider extends AbstractCacheProvider<AbstractRedisProvider> {

    public static final int DEFAULT_PUT_EXPIRATION_IN_SECONDS = 60;

    @Inject
    private Logger log;

    @Inject
    private CacheConfiguration cacheConfiguration;

    private AbstractRedisProvider redisProvider;
    private int defaultPutExpiration = DEFAULT_PUT_EXPIRATION_IN_SECONDS;

    public RedisProvider() {
    }

    @PostConstruct
    public void init() {
    }

    public void create() {
        try {
            RedisConfiguration redisConfiguration = cacheConfiguration.getRedisConfiguration();
            log.debug("Starting RedisProvider ... configuration:" + redisConfiguration);

            defaultPutExpiration = redisConfiguration.getDefaultPutExpiration() > 0 ? redisConfiguration.getDefaultPutExpiration() : DEFAULT_PUT_EXPIRATION_IN_SECONDS;

            redisProvider = RedisProviderFactory.create(cacheConfiguration.getRedisConfiguration());
            redisProvider.create();

            log.debug("RedisProvider started.");
        } catch (Exception e) {
            log.error("Failed to start RedisProvider.");
            throw new IllegalStateException("Error starting RedisProvider", e);
        }
    }

    @PreDestroy
    public void destroy() {
        log.debug("Destroying RedisProvider");
        redisProvider.destroy();
        log.debug("Destroyed RedisProvider");
    }

    @Override
    public AbstractRedisProvider getDelegate() {
        return redisProvider;
    }

    @Override
    public Object get(String region, String key) {
        if (key == null) {
            return null;
        }
        return redisProvider.get(key);
    }

    @Override // it is so weird but we use as workaround "region" field to pass "expiration" for put operation
    public void put(String expirationInSeconds, String key, Object object) {
        redisProvider.put(putExpiration(expirationInSeconds), key, object);
    }

    private int putExpiration(String expirationInSeconds) {
        try {
            return Integer.parseInt(expirationInSeconds);
        } catch (Exception e) {
            return defaultPutExpiration;
        }
    }

    @Override
    public void remove(String region, String key) {
        redisProvider.remove(key);
    }

    @Override
    public void clear() {
        redisProvider.clear();
    }
}
