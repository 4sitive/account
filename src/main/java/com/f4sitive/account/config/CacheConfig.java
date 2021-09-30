package com.f4sitive.account.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.cache.CacheType;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@EnableCaching(proxyTargetClass = true)
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CacheProperties.class)
public class CacheConfig {
    private final GenericApplicationContext context;
    private final CacheProperties properties;

    public CacheConfig(GenericApplicationContext context, CacheProperties properties) {
        this.context = context;
        this.properties = properties;
    }

    @Bean
    CacheManager cacheManager() {
        List<CacheManager> cacheManagers = Stream.concat(properties.getCacheNames().stream()
                                .filter(cacheName -> cacheName.indexOf('_') != -1)
                                .collect(LinkedMultiValueMap<CacheType, String>::new,
                                        (map, cacheName) -> map.add(CacheType.valueOf(cacheName.substring(0, cacheName.indexOf('_'))), cacheName.substring(cacheName.indexOf('_') + 1)),
                                        LinkedMultiValueMap::putAll)
                                .entrySet()
                                .stream()
                                .map(map -> {
                                    switch (map.getKey()) {
                                        case SIMPLE:
                                            ConcurrentMapCacheManager concurrentMapCacheManager = new ConcurrentMapCacheManager();
                                            concurrentMapCacheManager.setCacheNames(map.getValue());
                                            return concurrentMapCacheManager;
                                        case CAFFEINE:
                                            CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
                                            Optional.ofNullable(properties.getCaffeine().getSpec()).filter(StringUtils::hasText).ifPresent(caffeineCacheManager::setCacheSpecification);
                                            context.getBeanProvider(CaffeineSpec.class).ifAvailable(caffeineCacheManager::setCaffeineSpec);
                                            context.getBeanProvider(Caffeine.class).ifAvailable(caffeineCacheManager::setCaffeine);
                                            context.getBeanProvider(CacheLoader.class).ifAvailable(caffeineCacheManager::setCacheLoader);
                                            caffeineCacheManager.setCacheNames(map.getValue());
                                            return caffeineCacheManager;
                                        case REDIS:
                                            RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.builder(context.getBean(RedisConnectionFactory.class))
                                                    .cacheDefaults(context.getBeanProvider(RedisCacheConfiguration.class).getIfAvailable(() -> {
                                                        CacheProperties.Redis redisProperties = properties.getRedis();
                                                        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                                                                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new JdkSerializationRedisSerializer(context.getBean(ResourceLoader.class).getClassLoader())));
                                                        if (redisProperties.getTimeToLive() != null) {
                                                            config = config.entryTtl(redisProperties.getTimeToLive());
                                                        }
                                                        if (redisProperties.getKeyPrefix() != null) {
                                                            config = config.prefixCacheNameWith(redisProperties.getKeyPrefix());
                                                        }
                                                        if (!redisProperties.isCacheNullValues()) {
                                                            config = config.disableCachingNullValues();
                                                        }
                                                        if (!redisProperties.isUseKeyPrefix()) {
                                                            config = config.disableKeyPrefix();
                                                        }
                                                        return config;
                                                    }))
                                                    .initialCacheNames(new LinkedHashSet<>(map.getValue()));
                                            if (properties.getRedis().isEnableStatistics()) {
                                                builder.enableStatistics();
                                            }
                                            context.getBeanProvider(RedisCacheManagerBuilderCustomizer.class).orderedStream().forEach((customizer) -> customizer.customize(builder));
                                            return builder.build();
                                        default:
                                            return null;
                                    }
                                }),
                        Stream.of(RedisCacheManager.builder(context.getBean(RedisConnectionFactory.class))
                                        .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()
                                                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new Jackson2JsonRedisSerializer<>(JsonNode.class)))
                                                .entryTtl(Duration.ofDays(20L))
                                                .computePrefixWith(cacheName -> "TK-"))
                                        .initialCacheNames(Collections.singleton("TOKEN"))
                                        .disableCreateOnMissingCache()
                                        .build(),
                                new NoOpCacheManager()))
                .collect(Collectors.toList());
        cacheManagers.forEach(cacheManager -> context.registerBean(CacheManager.class.getName() + UUID.randomUUID(), CacheManager.class, () -> cacheManager, beanDefinition -> beanDefinition.setAutowireCandidate(false)));
        return new CompositeCacheManager(cacheManagers.stream().toArray(CacheManager[]::new));
    }
}
