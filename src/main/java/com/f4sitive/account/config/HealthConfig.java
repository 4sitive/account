package com.f4sitive.account.config;

import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.actuate.health.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.stream.Stream;

@Configuration(proxyBeanMethods = false)
public class HealthConfig {
    private Health health = Health.up().build();

    Health health(String health) {
        this.health = Stream.of("true", "on", "yes", "1", "t", "y", "o", "enable")
                .anyMatch(health::equalsIgnoreCase) ? Health.up().build() : Health.down().build();
        return this.health;
    }

    @Bean
    HealthIndicator healthIndicator() {
        return () -> health;
    }

    @Bean
    HealthEndpointWebExtension healthEndpointWebExtension(HealthContributorRegistry registry, HealthEndpointGroups groups) {
        return new HealthEndpointWebExtension(registry, groups) {
            @WriteOperation
            public Health health(@Selector String health) {
                return HealthConfig.this.health(health);
            }
        };
    }
}
