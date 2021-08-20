package com.f4sitive.account.config;

import lombok.Setter;
import net.logstash.logback.argument.StructuredArguments;
import net.ttddyy.dsproxy.listener.logging.DefaultQueryLogEntryCreator;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
@ConfigurationProperties(prefix = "sql")
public class SqlConfig {
    private final Logger log = LoggerFactory.getLogger("SQL");
    @Setter
    private long threshold;

    boolean isLogEnabled(long elapsedTime, boolean success, Throwable throwable) {
        return threshold <= elapsedTime || !success || throwable != null;
    }

    String format(String query) {
        switch (query.replaceAll("--.*\n", "").replace("\n", "").replaceAll("/\\*.*\\*/", "")
                .trim().split(" ", 2)[0].toUpperCase()) {
            case "SELECT":
            case "INSERT":
            case "UPDATE":
            case "DELETE":
                return FormatStyle.BASIC.getFormatter().format(query);
            case "CREATE":
            case "ALTER":
            case "DROP":
                return FormatStyle.DDL.getFormatter().format(query);
            default:
                return FormatStyle.NONE.getFormatter().format(query);
        }
    }

    @Bean
    BeanPostProcessor dataSourceBeanPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof DataSource) {
                    DefaultQueryLogEntryCreator creator = new DefaultQueryLogEntryCreator() {
                        @Override
                        protected String formatQuery(String query) {
                            return format(query);
                        }
                    };
                    creator.setMultiline(true);
                    return ProxyDataSourceBuilder
                            .create((DataSource) bean)
                            .proxyResultSet()
                            .traceMethodsWhen(log::isTraceEnabled, log::trace)
                            .afterQuery((execInfo, queryInfoList) -> {
                                long elapsedTime = execInfo.getElapsedTime();
                                boolean success = execInfo.isSuccess();
                                Throwable throwable = execInfo.getThrowable();
                                if (isLogEnabled(elapsedTime, success, throwable)) {
                                    log.info(creator.getLogEntry(execInfo, queryInfoList, false, true),
                                            StructuredArguments.keyValue("elapsed_time", elapsedTime),
                                            throwable);
                                }
                            })
                            .build();
                }
                return bean;
            }
        };
    }
}