/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.fineract.infrastructure.core.config.cache;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.config.FineractProperties;
import org.apache.logging.log4j.util.Strings;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.expiry.ExpiryPolicy;
import org.ehcache.jsr107.Eh107Configuration;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class CacheConfig {

    public static final String CONFIG_BY_NAME_CACHE_NAME = "configByName";

    private final FineractProperties fineractProperties;

    @Bean
    public TransactionBoundCacheManager defaultCacheManager(JCacheCacheManager ehCacheManager) {
        SpecifiedCacheSupportingCacheManager cacheManager = new SpecifiedCacheSupportingCacheManager();
        cacheManager.setNoOpCacheManager(new NoOpCacheManager());
        cacheManager.setDelegateCacheManager(ehCacheManager);
        cacheManager.setSupportedCaches(CONFIG_BY_NAME_CACHE_NAME);
        return new TransactionBoundCacheManager(cacheManager);
    }

    @Bean
    public JCacheCacheManager ehCacheManager() {
        JCacheCacheManager jCacheCacheManager = new JCacheCacheManager();
        jCacheCacheManager.setCacheManager(getInternalEhCacheManager());
        return jCacheCacheManager;
    }

    private CacheManager getInternalEhCacheManager() {
        CachingProvider provider = Caching.getCachingProvider();
        CacheManager cacheManager = provider.getCacheManager();

        javax.cache.configuration.Configuration<Object, Object> tenantTemplate = buildCacheConfig("tenant", 10000, ExpiryType.NO_EXPIRY,
                null);

        if (cacheManager.getCache("tenantsById") == null) {
            cacheManager.createCache("tenantsById", tenantTemplate);
        }

        javax.cache.configuration.Configuration<Object, Object> keyTemplate = buildCacheConfig("key", 10000, ExpiryType.TIME_TO_LIVE,
                Duration.ofMinutes(1));

        if (cacheManager.getCache("users") == null) {
            cacheManager.createCache("users", keyTemplate);
        }
        if (cacheManager.getCache("usersByUsername") == null) {
            cacheManager.createCache("usersByUsername", keyTemplate);
        }
        if (cacheManager.getCache("offices") == null) {
            cacheManager.createCache("offices", keyTemplate);
        }
        if (cacheManager.getCache("officesForDropdown") == null) {
            cacheManager.createCache("officesForDropdown", keyTemplate);
        }
        if (cacheManager.getCache("officesById") == null) {
            cacheManager.createCache("officesById", keyTemplate);
        }
        if (cacheManager.getCache("charges") == null) {
            cacheManager.createCache("charges", keyTemplate);
        }
        if (cacheManager.getCache("funds") == null) {
            cacheManager.createCache("funds", keyTemplate);
        }
        if (cacheManager.getCache("code_values") == null) {
            cacheManager.createCache("code_values", keyTemplate);
        }
        if (cacheManager.getCache("codes") == null) {
            cacheManager.createCache("codes", keyTemplate);
        }
        if (cacheManager.getCache("hooks") == null) {
            cacheManager.createCache("hooks", keyTemplate);
        }
        if (cacheManager.getCache("tfConfig") == null) {
            cacheManager.createCache("tfConfig", keyTemplate);
        }
        if (cacheManager.getCache(CONFIG_BY_NAME_CACHE_NAME) == null) {
            cacheManager.createCache(CONFIG_BY_NAME_CACHE_NAME, keyTemplate);
        }
        if (cacheManager.getCache("columnHeaders") == null) {
            cacheManager.createCache("columnHeaders", keyTemplate);
        }
        if (cacheManager.getCache("payment_types") == null) {
            cacheManager.createCache("payment_types", keyTemplate);
        }
        if (cacheManager.getCache("paymentTypesWithCode") == null) {
            cacheManager.createCache("paymentTypesWithCode", keyTemplate);
        }

        javax.cache.configuration.Configuration<Object, Object> tokenTemplate = buildCacheConfig("token", 10000, ExpiryType.TIME_TO_IDLE,
                Duration.ofHours(2));

        if (cacheManager.getCache("userTFAccessToken") == null) {
            cacheManager.createCache("userTFAccessToken", tokenTemplate);
        }

        return cacheManager;
    }

    @RequiredArgsConstructor
    @Getter
    public enum ExpiryType {

        NO_EXPIRY("noexpiry") {

            @Override
            public ExpiryPolicy<Object, Object> buildPolicy(Duration duration) {
                return ExpiryPolicyBuilder.noExpiration();
            }
        }, //
        TIME_TO_LIVE("timetolive") {

            @Override
            public ExpiryPolicy<Object, Object> buildPolicy(Duration duration) {
                return ExpiryPolicyBuilder.timeToLiveExpiration(duration);
            }
        }, //
        TIME_TO_IDLE("timetoidle") {

            @Override
            public ExpiryPolicy<Object, Object> buildPolicy(Duration duration) {
                return ExpiryPolicyBuilder.timeToIdleExpiration(duration);
            }
        }, //
        ;

        private final String name;

        private static final Map<String, ExpiryType> BY_NAME = Arrays.stream(values())
                .collect(Collectors.toMap(ExpiryType::getName, v -> v));

        public static ExpiryType byName(String name) {
            return Strings.isEmpty(name) ? null : BY_NAME.get(name.toLowerCase());
        }

        public abstract ExpiryPolicy<Object, Object> buildPolicy(Duration duration);
    }

    private javax.cache.configuration.Configuration<Object, Object> buildCacheConfig(@NotNull String cacheName, long defaultHeap,
            @NotNull ExpiryType defaultType, Duration defaultDuration) {
        Long heap = null;
        ExpiryType type = null;
        Duration duration = null;
        FineractProperties.FineractCacheProperties cacheConfig = fineractProperties.getCache();
        FineractProperties.FineractCacheInstance instance = cacheConfig == null ? null : cacheConfig.getInstance(cacheName);
        if (instance != null) {
            heap = instance.getHeap();
            String expiry = instance.getExpiry();
            if (!Strings.isEmpty(expiry)) {
                duration = Duration.parse("PT" + expiry);
            }
            type = ExpiryType.byName(instance.getExpiryPolicy());
        }
        if (heap == null) {
            heap = defaultHeap;
        }
        if (type == null) {
            type = defaultType;
        }
        if (duration == null) {
            duration = defaultDuration;
        }

        return Eh107Configuration.fromEhcacheCacheConfiguration(
                CacheConfigurationBuilder.newCacheConfigurationBuilder(Object.class, Object.class, ResourcePoolsBuilder.heap(heap))
                        .withExpiry(type.buildPolicy(duration)).build());
    }
}
