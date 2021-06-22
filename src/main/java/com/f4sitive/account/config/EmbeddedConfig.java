package com.f4sitive.account.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.embedded.RedisServer;

@Configuration
public class EmbeddedConfig {
    @ConditionalOnClass(RedisServer.class)
    @Bean(initMethod = "start", destroyMethod = "stop")
    public RedisServer redisContainer(RedisProperties properties) {
        return new RedisServer(properties.getPort());
    }
}
