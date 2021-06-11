package com.f4sitive.account.config;

import lombok.Getter;
import lombok.Setter;
import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
import org.apache.http.impl.execchain.ClientExecChain;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.net.ProxySelector;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

@Configuration(proxyBeanMethods = false)
@ConfigurationProperties("http")
public class HttpConfig {
    private final static Logger log = LoggerFactory.getLogger("HTTP");
    @Getter
    private final Proxy proxy = new Proxy();
    @Setter
    private Duration connectTimeout = Duration.ofSeconds(2L);
    @Setter
    private Duration readTimeout = Duration.ofSeconds(5L);
    @Setter
    private Duration idleTimeout = Duration.ofMinutes(10L);
    @Setter
    private Duration lifeTime = Duration.ofMinutes(30L);
    @Setter
    private Duration acquireTimeout = Duration.ofSeconds(0L);
    @Setter
    private long threshold;

    @Bean
    public CloseableHttpClient httpClient() {
        try {
            return new HttpClientBuilder() {
                @Override
                protected ClientExecChain decorateProtocolExec(ClientExecChain protocolExec) {
                    return (route, request, clientContext, execAware) -> {
                        CloseableHttpResponse response = null;
                        try {
                            return response = protocolExec.execute(route, request, clientContext, execAware);
                        } catch (Exception e) {
                            clientContext.setAttribute("throwable", e);
                            throw e;
                        } finally {
                            long elapsedTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - clientContext.getAttribute("elapsed_time", Long.class));
                            int statusCode = Optional.ofNullable(response)
                                    .map(res -> res.getStatusLine().getStatusCode())
                                    .orElse(0);
                            Throwable throwable = clientContext.getAttribute("throwable", Throwable.class);
                            if (threshold <= elapsedTime || throwable != null || Optional.ofNullable(HttpStatus.resolve(statusCode)).map(HttpStatus::isError).orElse(true)) {
                                StringBuilder requestContent = Optional.ofNullable(clientContext.getAttribute("request_content", String.class))
                                        .orElseGet(String::new)
                                        .codePoints()
                                        .limit(2048)
                                        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append);
                                HttpHeaders requestHeaders = Arrays.stream(request.getAllHeaders()).collect(HttpHeaders::new, (headers, header) -> headers.add(header.getName(), header.getValue()), HttpHeaders::putAll);
                                StringBuilder responseContent = Optional.ofNullable(clientContext.getAttribute("response_content", String.class))
                                        .orElseGet(String::new)
                                        .codePoints()
                                        .limit(2048)
                                        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append);
                                HttpHeaders responseHeaders = Optional.ofNullable(response)
                                        .map(res -> Arrays.stream(res.getAllHeaders()).collect(HttpHeaders::new, (headers, header) -> headers.add(header.getName(), header.getValue()), HttpHeaders::putAll))
                                        .orElseGet(HttpHeaders::new);
                                log.info("elapsed_time: {}, method: {}, requested_uri: {}, status_code: {}, request_headers: {}, response_headers: {}, request_content: {}, response_content: {}",
                                        elapsedTime,
                                        request.getMethod(),
                                        request.getOriginal().getRequestLine().getUri(),
                                        statusCode,
                                        requestHeaders,
                                        responseHeaders,
                                        requestContent,
                                        responseContent,
                                        throwable
                                );
                            }
                        }
                    };
                }
            }
                    .setSSLContext(SSLContextBuilder
                            .create()
                            .loadTrustMaterial((TrustStrategy) (chain, authType) -> true)
                            .build())
                    .setDefaultRequestConfig(RequestConfig.custom().setConnectTimeout((int) connectTimeout.toMillis()).setConnectionRequestTimeout((int) acquireTimeout.toMillis()).setSocketTimeout((int) readTimeout.toMillis()).build())
                    .setSSLHostnameVerifier((hostname, session) -> true)
                    .setMaxConnTotal(1000)
                    .setMaxConnPerRoute(100)
                    .setConnectionTimeToLive(lifeTime.toMillis(), TimeUnit.MILLISECONDS)
                    .setRoutePlanner(new SystemDefaultRoutePlanner(ProxySelector.getDefault()) {
                        private final HttpHost proxy = new HttpHost(getProxy().getHost(), getProxy().getPort());

                        @Override
                        protected HttpHost determineProxy(HttpHost target, HttpRequest request, HttpContext context) throws HttpException {
                            return Optional.ofNullable(super.determineProxy(target, request, context))
                                    .orElseGet(() -> getProxy().matches(target.getHostName()) ? this.proxy : null);
                        }
                    })
                    .useSystemProperties()
                    .disableRedirectHandling()
                    .disableAutomaticRetries()
                    .evictIdleConnections(idleTimeout.toMillis(), TimeUnit.MILLISECONDS)
                    .evictExpiredConnections()
                    .addInterceptorFirst((HttpRequestInterceptor) (request, context) -> context.setAttribute("elapsed_time", System.nanoTime()))
                    .addInterceptorLast((HttpRequestInterceptor) (request, context) -> {
                        if (request instanceof HttpEntityEnclosingRequest) {
                            context.setAttribute("request_content", EntityUtils.toString(((HttpEntityEnclosingRequest) request).getEntity()));
                        }
                    })
                    .addInterceptorLast((HttpResponseInterceptor) (response, context) -> {
                        if (response.getEntity() != null) {
                            EntityUtils.updateEntity(response, new ByteArrayEntity(EntityUtils.toByteArray(response.getEntity()), ContentType.get(response.getEntity())));
                            context.setAttribute("response_content", EntityUtils.toString(response.getEntity()));
                        }
                    })
                    .build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }

    @Getter
    @Setter
    public static class Proxy {
        private String host = "localhost";
        private int port = -1;
        private Set<String> included = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        private Set<String> excluded = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        public boolean matches(String host) {
            if ("localhost".equals(this.host) || this.port == -1) {
                return false;
            }
            return this.included.stream().anyMatch(included -> included.equals(host)) && this.excluded.stream().noneMatch(excluded -> excluded.equals(host));
        }
    }
}