package com.f4sitive.account.config;

import com.f4sitive.account.util.Snowflakes;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration(proxyBeanMethods = false)
public class DataConfig {
    @Bean
    Snowflakes snowflakes(@Value("${spring.application.index:${spring.cloud.client.ip-address:${random.long[1,1023]}}}") String id) {
        return new Snowflakes(Long.parseLong(id.chars().filter(Character::isDigit).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString()));
    }
}
