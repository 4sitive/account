package com.f4sitive.account.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@ConfigurationProperties("cors")
@Configuration(proxyBeanMethods = false)
public class CorsConfig {
    @Getter
    private final Map<String, Cors> mapping = new LinkedHashMap<>();

    @Bean
    CorsFilter corsFilter(List<CorsConfigurationSource> sources) {
        return new CorsFilter(request -> sources.stream().map(source -> source.getCorsConfiguration(request)).filter(Objects::nonNull).findFirst().orElse(null));
    }

    @Bean
    org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.setCorsConfigurations(mapping.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        return source;
    }

    public static class Cors extends CorsConfiguration {
        Cors() {
            applyPermitDefaultValues();
        }
    }
}
