/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.xdi.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.xdi.service.cache.CacheProvider;
import org.xdi.service.cache.NativePersistenceCacheProvider;

/**
 * Provides operations with cache
 * 
 * @author Yuriy Movchan Date: 01.24.2012
 * @author Yuriy Zabrovarnyy Date: 02.02.2017
 */
@ApplicationScoped
@Named
public class CacheService {

	@Inject
	private CacheProvider cacheProvider;

	@Inject
    private Logger log;

	public Object get(String region, String key) {
		if (cacheProvider == null) {
			return null;
		}
		
		return cacheProvider.get(region, key);
	}

	public void put(String region, String key, Object object) {
		if (cacheProvider != null) {
			cacheProvider.put(region, key, object);
		}
	}

	public void remove(String region, String key) {
		if (cacheProvider == null) {
			return;
		}
		
		cacheProvider.remove(region, key);
	}

	@Deprecated // todo we must not stick to ehcache specific classes ! Scheduled for removing!
	public void removeAll(String name) {
		cacheProvider.clear(); // for non ehcache clear all cache (e.g. in memcache we don't have regions)
	}

	public void clear() {
		if (cacheProvider != null) {
			cacheProvider.clear();
		}
	}

	public CacheProvider<?> getCacheProvider() {
		return cacheProvider;
	}

	public boolean isNativePersistenceCacheProvider() {
		return cacheProvider instanceof NativePersistenceCacheProvider;
	}

}
