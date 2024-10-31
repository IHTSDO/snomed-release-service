package org.ihtsdo.buildcloud.core.service.browser.update;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.ihtsdo.sso.integration.SecurityUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

@Service
public class BrowserUpdateClientFactory {

    @Value("${snowstorm.browser-update.url}")
    private String browserUpdateTermServerUrl;

    private final Cache<String, BrowserUpdateClient> clientCache;

    public BrowserUpdateClientFactory() {
        this.clientCache = CacheBuilder.newBuilder().expireAfterAccess(5L, TimeUnit.MINUTES).build();
    }

    public BrowserUpdateClient getClient() {
        BrowserUpdateClient client = null;
        String authenticationToken = SecurityUtil.getAuthenticationToken();
        if (StringUtils.hasLength(authenticationToken)) {
            client = this.clientCache.getIfPresent(authenticationToken);
        }
        if (client == null) {
            synchronized (this.clientCache) {
                authenticationToken = SecurityUtil.getAuthenticationToken();
                if (StringUtils.hasLength(authenticationToken)) {
                    client = this.clientCache.getIfPresent(authenticationToken);
                }
                if (client == null) {
                    client = new BrowserUpdateClient(browserUpdateTermServerUrl, authenticationToken);
                    authenticationToken = SecurityUtil.getAuthenticationToken();
                    this.clientCache.put(authenticationToken, client);
                }
            }
        }

        return client;
    }
}

