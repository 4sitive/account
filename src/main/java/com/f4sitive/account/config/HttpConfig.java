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
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpProxy;
import org.eclipse.jetty.client.Origin;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    private Duration acquireTimeout = Duration.ofSeconds(2L);
    @Setter
    private Duration queryTimeout = Duration.ofSeconds(2L);
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
                    .setDefaultRequestConfig(RequestConfig
                            .custom()
                            .setConnectTimeout((int) connectTimeout.toMillis())
                            .setSocketTimeout((int) readTimeout.toMillis())
                            .setConnectionRequestTimeout((int) acquireTimeout.toMillis())
                            .build())
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
                    .setDnsResolver(host -> {
                        try {
                            return CompletableFuture.supplyAsync(() -> {
                                try {
                                    return InetAddress.getAllByName(host);
                                } catch (UnknownHostException e) {
                                    throw new RuntimeException(e);
                                }
                            }).get(queryTimeout.toMillis(), TimeUnit.MILLISECONDS);
                        } catch (InterruptedException | TimeoutException e) {
                            UnknownHostException exception = new UnknownHostException(e instanceof TimeoutException ? "DNS timeout " + queryTimeout.toMillis() + " ms" : e.getMessage());
                            exception.setStackTrace(e.getStackTrace());
                            exception.initCause(e);
                            throw exception;
                        } catch (ExecutionException e) {
                            throw Optional.ofNullable(e.getCause())
                                    .filter(throwable -> throwable.getCause() instanceof UnknownHostException)
                                    .map(throwable -> (UnknownHostException) throwable.getCause())
                                    .orElseGet(() -> {
                                        UnknownHostException exception = new UnknownHostException("Search domain query failed. Original hostname: '" + host + "' ");
                                        exception.setStackTrace(e.getStackTrace());
                                        exception.initCause(e.getCause());
                                        return exception;
                                    });
                        }
                    })
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

    @Bean
    public HttpClient reactiveHttpClient() {
        HttpClient httpClient = new HttpClient(new SslContextFactory.Client(true)) {
            @Override
            public Request newRequest(URI uri) {
                Request newRequest = super.newRequest(uri);
                newRequest.attribute("elapsed_time", System.nanoTime());
                newRequest.onRequestContent((request, content) -> {
                    try (InputStream inputStream = DefaultDataBufferFactory.sharedInstance.wrap(content.asReadOnlyBuffer()).asInputStream()) {
                        request.attribute("request_content", new String(StreamUtils.copyToByteArray(inputStream)));
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                });
                newRequest.onResponseContent((response, content) -> {
                    try (InputStream inputStream = DefaultDataBufferFactory.sharedInstance.wrap(content.asReadOnlyBuffer()).asInputStream()) {
                        response.getRequest().attribute("response_content", new String(StreamUtils.copyToByteArray(inputStream)));
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                });
                newRequest.onComplete(complete -> {
                    long elapsedTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - (long) complete.getRequest().getAttributes().get("elapsed_time"));
                    int statusCode = complete.getResponse().getStatus();
                    if (threshold <= elapsedTime || complete.getFailure() != null || Optional.ofNullable(HttpStatus.resolve(statusCode)).map(HttpStatus::isError).orElse(true)) {
                        StringBuilder requestContent = Optional.ofNullable((String) complete.getRequest().getAttributes().get("request_content"))
                                .orElseGet(String::new)
                                .codePoints()
                                .limit(2048)
                                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append);
                        HttpHeaders requestHeaders = complete.getRequest().getHeaders().stream().collect(HttpHeaders::new, (headers, httpField) -> headers.add(httpField.getName(), httpField.getValue()), HttpHeaders::putAll);
                        StringBuilder responseContent = Optional.ofNullable((String) complete.getRequest().getAttributes().get("response_content"))
                                .orElseGet(String::new)
                                .codePoints()
                                .limit(2048)
                                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append);
                        HttpHeaders responseHeaders = complete.getResponse().getHeaders().stream().collect(HttpHeaders::new, (headers, httpField) -> headers.add(httpField.getName(), httpField.getValue()), HttpHeaders::putAll);
                        log.info("elapsed_time: {}, method: {}, requested_uri: {}, status_code: {}, request_headers: {}, response_headers: {}, request_content: {}, response_content: {}",
                                elapsedTime,
                                complete.getRequest().getMethod(),
                                complete.getRequest().getURI(),
                                statusCode,
                                requestHeaders,
                                responseHeaders,
                                requestContent,
                                responseContent,
                                complete.getFailure()
                        );
                    }
                });
                return newRequest;
            }
        };
        httpClient.getProxyConfiguration().getProxies().add(new HttpProxy(proxy.getHost(), proxy.getPort()) {
            @Override
            public boolean matches(Origin origin) {
                return proxy.matches(origin.getAddress().getHost());
            }
        });
        httpClient.setConnectTimeout(connectTimeout.toMillis());
        httpClient.setAddressResolutionTimeout(queryTimeout.toMillis());
        httpClient.setIdleTimeout(idleTimeout.toMillis());
        httpClient.setFollowRedirects(false);
        httpClient.setUserAgentField(null);
        return httpClient;
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