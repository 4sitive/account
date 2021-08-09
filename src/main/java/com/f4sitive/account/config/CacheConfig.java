package com.f4sitive.account.config;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.SerializationException;

import java.time.Duration;
import java.util.Collections;

@Slf4j
public class CacheConfig {
    @Bean
    Cache token(RedisConnectionFactory redisConnectionFactory) {
        String name = "TOKEN";
        RedisCacheManager cacheManager = RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig()
                        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new Jackson2JsonRedisSerializer<>(JsonNode.class)))
                        .entryTtl(Duration.ofDays(20L))
                        .computePrefixWith(cacheName -> "TK-"))
                .initialCacheNames(Collections.singleton(name))
                .disableCreateOnMissingCache()
                .build();
        cacheManager.afterPropertiesSet();
        return cacheManager.getCache(name);
    }

    @Bean
    Cache authorizationCode(RedisConnectionFactory redisConnectionFactory, ResourceLoader resourceLoader) {
        String name = "AUTHORIZATION_CODE";
        RedisCacheManager cacheManager = RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig()
                        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new JdkSerializationRedisSerializer(resourceLoader.getClassLoader()) {
                            @Override
                            public Object deserialize(byte[] bytes) {
                                try {
                                    return super.deserialize(bytes);
                                } catch (SerializationException e) {
                                    log.error("Cache deserialize fail... {}", e.getMessage(), e);
                                    return null;
                                }
                            }
                        }))
                        .entryTtl(Duration.ofHours(1L))
                        .computePrefixWith(cacheName -> name + ":")
                        .disableCachingNullValues())
                .initialCacheNames(Collections.singleton(name))
                .disableCreateOnMissingCache()
                .transactionAware()
                .build();
        cacheManager.afterPropertiesSet();
        return cacheManager.getCache(name);
    }
}
