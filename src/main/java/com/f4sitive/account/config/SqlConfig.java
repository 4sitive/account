package com.f4sitive.account.config;

import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.P6DataSource;
import com.p6spy.engine.spy.P6SpyOptions;
import com.p6spy.engine.spy.appender.FormattedLogger;
import lombok.Getter;
import lombok.Setter;
import net.ttddyy.dsproxy.listener.logging.DefaultQueryLogEntryCreator;
import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.listener.logging.SystemOutQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import javax.sql.DataSource;
import java.util.function.Supplier;

@Configuration
@ConfigurationProperties(prefix = "sql")
public class SqlConfig {
    private final static Logger log = LoggerFactory.getLogger("SQL");
    @Getter
    @Setter
    private long threshold;

    @Bean
    @Order
    public BeanPostProcessor dataSourceBeanPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof DataSource) {
                    DefaultQueryLogEntryCreator creator = new DefaultQueryLogEntryCreator() {
                        @Override
                        protected String formatQuery(String query) {
                            switch (query.replaceAll("--.*\n", "").replaceAll("\n", "").replaceAll("/\\*.*\\*/", "").trim().split(" ", 2)[0].toUpperCase()) {
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
                    };
                    creator.setMultiline(true);
                    return new LazyConnectionDataSourceProxy(ProxyDataSourceBuilder
                            .create((DataSource) bean)
                            .proxyResultSet()
                            .traceMethodsWhen(() -> log.isTraceEnabled(), (message) -> log.trace(message))
                            .afterQuery((execInfo, queryInfoList) -> {
                                if (threshold <= execInfo.getElapsedTime()) {
                                    log.info(creator.getLogEntry(execInfo, queryInfoList, false, true));
                                }
                            })
                            .build());
//                    P6SpyOptions.getActiveInstance().setAppender(SqlLogger.class.getName());
//                    if (threshold > 0L) {
//                        ((SqlLogger) P6SpyOptions.getActiveInstance().getAppenderInstance()).setThreshold(threshold);
//                    }
//                    return new LazyConnectionDataSourceProxy(new P6DataSource((DataSource) bean));
                }
                return bean;
            }
        };
    }

    public static class SqlLogger extends FormattedLogger {
        @Setter
        private long threshold;

        @Override
        public void logException(Exception e) {
            log.info("", e);
        }

        @Override
        public void logText(String text) {
            log.info(text);
        }

        @Override
        public void logSQL(int connectionId, String now, long elapsed, Category category, String prepared, String sql, String url) {
            Supplier<String> query = () -> {
                if (Category.STATEMENT.equals(category) || Category.BATCH.equals(category)) {
                    switch (sql.replaceAll("--.*\n", "").replaceAll("\n", "").replaceAll("/\\*.*\\*/", "").trim().split(" ", 2)[0].toUpperCase()) {
                        case "SELECT":
                        case "INSERT":
                        case "UPDATE":
                        case "DELETE":
                            return FormatStyle.BASIC.getFormatter().format(sql);
                        case "CREATE":
                        case "ALTER":
                        case "DROP":
                            return FormatStyle.DDL.getFormatter().format(sql);
                        default:
                            return FormatStyle.NONE.getFormatter().format(sql);
                    }
                } else {
                    return prepared;
                }
            };
            String msg = strategy.formatMessage(connectionId, now, elapsed, category.toString(), "", query.get(), url);
            if (Category.ERROR.equals(category)) {
                log.error(msg);
            } else if (Category.WARN.equals(category)) {
                log.warn(msg);
            } else if (threshold <= elapsed) {
                log.info(msg);
            }
        }

        @Override
        public boolean isCategoryEnabled(Category category) {
            if (Category.ERROR.equals(category)) {
                return log.isErrorEnabled();
            } else if (Category.WARN.equals(category)) {
                return log.isWarnEnabled();
            } else {
                return log.isInfoEnabled();
            }
        }
    }
}