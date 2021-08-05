package com.f4sitive.account.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.MySQLContainer;

import java.util.Collections;
import java.util.Optional;

@Profile("localhost")
@Configuration(proxyBeanMethods = false)
public class TestConfig {
    @Bean
    @Order
    public BeanPostProcessor dataSourcePropertiesBeanPostProcessor(MySQLContainer<?> mySqlContainer) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof DataSourceProperties) {
                    UriComponentsBuilder ssp = Optional.ofNullable(((DataSourceProperties) bean).getUrl())
                            .map(url -> UriComponentsBuilder
                                    .fromUriString(UriComponentsBuilder
                                            .fromUriString(url)
                                            .build(Collections.emptyMap())
                                            .getSchemeSpecificPart())
                                    .host(mySqlContainer.getHost())
                                    .port(mySqlContainer.getMappedPort(MySQLContainer.MYSQL_PORT))
                                    .replacePath(mySqlContainer.getDatabaseName()))
                            .orElseGet(() -> UriComponentsBuilder
                                    .fromUriString(UriComponentsBuilder
                                            .fromUriString(mySqlContainer.getJdbcUrl())
                                            .build(Collections.emptyMap())
                                            .getSchemeSpecificPart()));
                    ((DataSourceProperties) bean).setUrl(UriComponentsBuilder.newInstance()
                            .scheme("jdbc")
                            .schemeSpecificPart(ssp.toUriString())
                            .toUriString());
                    ((DataSourceProperties) bean).setUsername(mySqlContainer.getUsername());
                    ((DataSourceProperties) bean).setPassword(mySqlContainer.getPassword());
                }
                return bean;
            }
        };
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public MySQLContainer<?> mySqlContainer() {
        MySQLContainer<?> container = new MySQLContainer<>("mysql:5.7.35");
        container.setCommand(
                "--character-set-server=utf8mb4",
                "--collation-server=utf8mb4_unicode_ci",
                "--lower_case_table_names=1"
        );
        return container;
    }
}
