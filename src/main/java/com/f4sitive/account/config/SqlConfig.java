package com.f4sitive.account.config;

import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.P6DataSource;
import com.p6spy.engine.spy.P6SpyOptions;
import com.p6spy.engine.spy.appender.FormattedLogger;
import lombok.Getter;
import lombok.Setter;
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
                    P6SpyOptions.getActiveInstance().setAppender(SqlLogger.class.getName());
                    if (threshold > 0L) {
                        ((SqlLogger) P6SpyOptions.getActiveInstance().getAppenderInstance()).setThreshold(threshold);
                    }
                    return new LazyConnectionDataSourceProxy(new P6DataSource((DataSource) bean));
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
            if (Category.STATEMENT.equals(category) || Category.BATCH.equals(category)) {
                switch (sql.trim().split(" ", 2)[0].toUpperCase()) {
                    case "SELECT":
                    case "INSERT":
                    case "UPDATE":
                    case "DELETE":
                        sql = FormatStyle.BASIC.getFormatter().format(sql);
                        break;
                    case "CREATE":
                    case "ALTER":
                    case "DROP":
                        sql = FormatStyle.DDL.getFormatter().format(sql);
                        break;
                    default:
                        sql = FormatStyle.NONE.getFormatter().format(sql);
                        break;
                }
                prepared = "";
            }

            String msg = strategy.formatMessage(connectionId, now, elapsed, category.toString(), prepared, sql, url);

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