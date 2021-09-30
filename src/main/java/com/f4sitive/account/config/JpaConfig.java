package com.f4sitive.account.config;

import com.f4sitive.account.util.Snowflakes;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@Configuration(proxyBeanMethods = false)
public class JpaConfig {
    @Bean
    HibernatePropertiesCustomizer hibernatePropertiesCustomizer(Snowflakes snowflakes) {
        return hibernateProperties -> hibernateProperties.put("snowflakes", snowflakes);
    }
}
