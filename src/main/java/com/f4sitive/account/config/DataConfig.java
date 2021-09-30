package com.f4sitive.account.config;

import com.f4sitive.account.util.Snowflakes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class DataConfig {
    @Bean
    Snowflakes snowflakes(@Value("${spring.application.index:${spring.cloud.client.ip-address:${random.long[0,1023]}}}") String id) {
        return new Snowflakes(Long.parseUnsignedLong(id.chars().filter(Character::isDigit).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString()));
    }
}