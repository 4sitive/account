package com.foursitive.account.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.testcontainers.containers.MySQLContainer;

@Configuration
@Profile({"test"})
public class TestContainerConfig {
    @ConditionalOnClass(MySQLContainer.class)
    @Configuration
    @Profile({"test"})
    protected static class MySQLContainerConfiguration {
        @Bean
        @Order
        public BeanPostProcessor dataSourcePropertiesBeanPostProcessor() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                    if (bean instanceof DataSourceProperties) {
                        MySQLContainer mySQLContainer = mySQLContainer();
                        ((DataSourceProperties) bean).setUrl(mySQLContainer.getJdbcUrl() + "?allowMultiQueries=true&connectTimeout=2000&socketTimeout=10000&autoReconnect=true&autoReconnectForPools=true&useSSL=false&useUnicode=yes&characterEncoding=UTF-8&serverTimezone=Asia/Seoul");
                        ((DataSourceProperties) bean).setUsername(mySQLContainer.getUsername());
                        ((DataSourceProperties) bean).setPassword(mySQLContainer.getPassword());
                    }
                    return bean;
                }
            };
        }

        @Bean(initMethod = "start", destroyMethod = "stop")
        public MySQLContainer mySQLContainer() {
            return new MySQLContainer("mysql:5.7.22") {
                {
                    setCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci", "--lower_case_table_names=1");
                }
            };
        }
    }
}
