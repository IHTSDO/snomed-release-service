package org.ihtsdo.buildcloud.config;

import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class SimpleCacheManagerConfiguration {

	@Bean
	public SimpleCacheManager cacheManager() {
		final SimpleCacheManager cacheManager = new SimpleCacheManager();
		cacheManager.setCaches(Arrays.asList(getCache("release-center-records"),
				getCache("global-roles"), getCache("code-system-roles")));
		return cacheManager;
	}

	private Cache getCache(final String name) {
		return new ConcurrentMapCache(name);
	}
}
