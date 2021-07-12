package com.f4sitive.account.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
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
    FilterRegistrationBean<Filter> corsFilter(List<CorsConfigurationSource> sources) {
        FilterRegistrationBean<Filter> filterRegistrationBean = new FilterRegistrationBean<>(new CorsFilter(request -> sources.stream().map(source -> source.getCorsConfiguration(request)).filter(Objects::nonNull).findFirst().orElse(null)));
        filterRegistrationBean.setDispatcherTypes(DispatcherType.ASYNC, DispatcherType.ERROR, DispatcherType.REQUEST);
        filterRegistrationBean.setName("corsFilter");
        return filterRegistrationBean;
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
