package com.f4sitive.account.config;

import com.f4sitive.account.entity.Constants;
import com.f4sitive.account.util.Snowflakes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.WebRequest;

import java.util.Optional;

@Configuration(proxyBeanMethods = false)
public class DataConfig {
    @Bean
    Snowflakes snowflakes(@Value("${spring.application.index:${spring.cloud.client.ip-address:${random.long[0,1023]}}}") String id) {
        return new Snowflakes(Long.parseUnsignedLong(id.chars().filter(Character::isDigit).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString()));
    }

    @Bean
    AuditorAware<String> auditorProvider(Optional<WebRequest> optional) {
        return () -> {
            String auditor = optional
                    .map(request -> {
                        try {
                            return request.getHeader("User-Id");
                        } catch (IllegalStateException e) {
                        }
                        return null;
                    })
                    .filter(StringUtils::hasText)
                    .orElseGet(() -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                            .filter(authentication -> authentication.getDetails() != null)
                            .map(Authentication::getName)
                            .orElse(null));
            return Optional.ofNullable(auditor)
                    .map(name -> name.substring(0, Math.min(Constants.ID_LENGTH, name.length())));
        };
    }

}