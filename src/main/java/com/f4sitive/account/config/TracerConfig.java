package com.f4sitive.account.config;

import brave.baggage.*;
import ch.qos.logback.classic.ClassicConstants;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

@Configuration(proxyBeanMethods = false)
public class TracerConfig {
    BaggageField requestRequestUri = BaggageField.create(ClassicConstants.REQUEST_REQUEST_URI);
    BaggageField requestQueryString = BaggageField.create(ClassicConstants.REQUEST_QUERY_STRING);
    BaggageField requestMethod = BaggageField.create(ClassicConstants.REQUEST_METHOD);

    @Bean
    FilterRegistrationBean<Filter> tracerFilter(Optional<Tracer> tracer) {
        FilterRegistrationBean<Filter> filterRegistrationBean = new FilterRegistrationBean<>((request, response, chain) -> {
            HttpServletRequest req = (HttpServletRequest) request;
            HttpServletResponse res = (HttpServletResponse) response;
            tracer.ifPresent(t -> {
                Optional.ofNullable(t.currentSpan()).map(Span::context).ifPresent(context -> {
                    Optional.ofNullable(context.traceId()).ifPresent(traceId -> res.addHeader("traceId", traceId));
                    Optional.ofNullable(context.spanId()).ifPresent(spanId -> res.addHeader("spanId", spanId));
                    Optional.ofNullable(context.parentId()).ifPresent(parentId -> res.addHeader("parentId", parentId));
                });
                t.createBaggage(ClassicConstants.REQUEST_REQUEST_URI, req.getRequestURI());
                t.createBaggage(ClassicConstants.REQUEST_QUERY_STRING, req.getQueryString());
                t.createBaggage(ClassicConstants.REQUEST_METHOD, req.getMethod());
            });
            chain.doFilter(request, response);
        });
        filterRegistrationBean.setDispatcherTypes(DispatcherType.REQUEST);
        filterRegistrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        filterRegistrationBean.setName("tracerFilter");
        return filterRegistrationBean;
    }

    @Bean
    BaggagePropagationCustomizer baggagePropagationCustomizer() {
        return builder -> builder
                .add(BaggagePropagationConfig.SingleBaggageField.local(requestRequestUri))
                .add(BaggagePropagationConfig.SingleBaggageField.local(requestQueryString))
                .add(BaggagePropagationConfig.SingleBaggageField.local(requestMethod));
    }

    @Bean
    CorrelationScopeCustomizer correlationScopeCustomizer() {
        return builder -> builder
                .add(CorrelationScopeConfig.SingleCorrelationField.create(BaggageFields.PARENT_ID))
                .add(CorrelationScopeConfig.SingleCorrelationField.create(BaggageFields.SAMPLED))
                .add(CorrelationScopeConfig.SingleCorrelationField.newBuilder(requestRequestUri).flushOnUpdate().build())
                .add(CorrelationScopeConfig.SingleCorrelationField.newBuilder(requestQueryString).flushOnUpdate().build())
                .add(CorrelationScopeConfig.SingleCorrelationField.newBuilder(requestMethod).flushOnUpdate().build());
    }
}
