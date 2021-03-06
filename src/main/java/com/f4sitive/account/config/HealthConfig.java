package com.f4sitive.account.config;

import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.boot.actuate.health.HealthEndpointWebExtension;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Configuration(proxyBeanMethods = false)
public class HealthConfig {
    private Health health = Health.up().build();
    private final Optional<Registration> registration;
    private final Optional<ServiceRegistry<?>> serviceRegistry;

    HealthConfig(Optional<Registration> registration, Optional<ServiceRegistry<?>> serviceRegistry) {
        this.registration = registration;
        this.serviceRegistry = serviceRegistry;
    }

    @SuppressWarnings("unchecked")
    Health health(String health) {
        this.health = Stream.of("true", "on", "yes", "1", "t", "y", "o", "enable").anyMatch(health::equalsIgnoreCase) ? Health.up().build() : Health.down().build();
        this.serviceRegistry
                .map(ServiceRegistry.class::cast)
                .<Consumer<Registration>>map(sr -> Status.UP.equals(this.health.getStatus()) ? sr::register : sr::deregister)
                .ifPresent(this.registration::ifPresent);
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
