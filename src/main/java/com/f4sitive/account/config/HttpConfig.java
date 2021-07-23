package com.f4sitive.account.config;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.logstash.logback.argument.StructuredArguments;
import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
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

import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

@Configuration(proxyBeanMethods = false)
@ConfigurationProperties("http")
public class HttpConfig {
    private final Logger log = LoggerFactory.getLogger("HTTP");
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
    private Duration acquireTimeout = Duration.ofSeconds(1L);
    @Setter
    private Duration queryTimeout = Duration.ofSeconds(3L);
    @Setter
    private long threshold;

    boolean isLogEnabled(long elapsedTime, boolean error, Throwable throwable) {
        return threshold <= elapsedTime || error || throwable != null;
    }

    @SneakyThrows
    @Bean
    public CloseableHttpClient httpClient() {
        String elapsedTimeName = "elapsed_time";
        String requestContentName = "request_content";
        String responseContentName = "response_content";
        String statusCodeName = "status_code";
        String requestHeadersName = "request_headers";
        String responseHeadersName = "response_headers";
        String throwableName = "throwable";
        HttpRequestInterceptor httpRequestInterceptorFirst = (request, context) ->
                context.setAttribute(elapsedTimeName, System.nanoTime());
        HttpRequestInterceptor httpRequestInterceptorLast = (request, context) -> {
            context.setAttribute(requestHeadersName, Arrays.stream(request.getAllHeaders())
                    .collect(HttpHeaders::new,
                            (headers, header) -> headers.add(header.getName(), header.getValue()),
                            HttpHeaders::putAll));
            if (request instanceof HttpEntityEnclosingRequest) {
                String content = EntityUtils.toString(((HttpEntityEnclosingRequest) request).getEntity());
                context.setAttribute(requestContentName, content);
            }
        };
        HttpResponseInterceptor httpResponseInterceptorLast = (response, context) -> {
            ByteArrayEntity entity = new ByteArrayEntity(EntityUtils.toByteArray(response.getEntity()),
                    ContentType.get(response.getEntity()));
            EntityUtils.updateEntity(response, entity);
            context.setAttribute(responseContentName, EntityUtils.toString(entity));
            context.setAttribute(statusCodeName, response.getStatusLine().getStatusCode());
            context.setAttribute(responseHeadersName, Arrays.stream(response.getAllHeaders())
                    .collect(HttpHeaders::new,
                            (headers, header) -> headers.add(header.getName(), header.getValue()),
                            HttpHeaders::putAll));
        };
        return new HttpClientBuilder() {
            @Override
            protected ClientExecChain decorateProtocolExec(ClientExecChain protocolExec) {
                return (route, request, clientContext, execAware) -> {
                    try {
                        return protocolExec.execute(route, request, clientContext, execAware);
                    } catch (Exception e) {
                        clientContext.setAttribute(throwableName, e);
                        throw e;
                    } finally {
                        long elapsedTime = TimeUnit.NANOSECONDS
                                .toMillis(System.nanoTime() - clientContext
                                        .getAttribute(elapsedTimeName, Long.class));
                        int statusCode = Optional.ofNullable(clientContext.getAttribute(statusCodeName, Integer.class))
                                .orElse(0);
                        boolean error = Optional.ofNullable(HttpStatus.resolve(statusCode)).map(HttpStatus::isError)
                                .orElse(true);
                        Throwable throwable = clientContext.getAttribute(throwableName, Throwable.class);
                        if (isLogEnabled(elapsedTime, error, throwable)) {
                            String requestContent = Optional.ofNullable(clientContext
                                    .getAttribute(requestContentName, String.class))
                                    .orElseGet(String::new);
                            HttpHeaders requestHeaders = Optional.ofNullable(clientContext
                                    .getAttribute(requestHeadersName, HttpHeaders.class))
                                    .orElseGet(HttpHeaders::new);
                            String responseContent = Optional.ofNullable(clientContext
                                    .getAttribute(responseContentName, String.class))
                                    .orElseGet(String::new);
                            HttpHeaders responseHeaders = Optional.ofNullable(clientContext
                                    .getAttribute(responseHeadersName, HttpHeaders.class))
                                    .orElseGet(HttpHeaders::new);
                            log.info("{},{},{},{},{},{},{},{}",
                                    StructuredArguments.keyValue(elapsedTimeName, elapsedTime),
                                    StructuredArguments.keyValue("method", request.getOriginal().getRequestLine()
                                            .getMethod()),
                                    StructuredArguments.keyValue("requested_uri", request.getOriginal().getRequestLine()
                                            .getUri()),
                                    StructuredArguments.keyValue(statusCodeName, statusCode),
                                    StructuredArguments.keyValue(requestHeadersName, requestHeaders),
                                    StructuredArguments.keyValue(responseHeadersName, responseHeaders),
                                    StructuredArguments.keyValue(requestContentName, requestContent),
                                    StructuredArguments.keyValue(responseContentName, responseContent),
                                    throwable
                            );
                        }
                    }
                };
            }
        }
                .setSSLContext(SSLContextBuilder
                        .create()
                        .loadTrustMaterial(TrustAllStrategy.INSTANCE)
                        .build())
                .setDefaultRequestConfig(RequestConfig
                        .custom()
                        .setConnectTimeout((int) connectTimeout.toMillis())
                        .setSocketTimeout((int) readTimeout.toMillis())
                        .setConnectionRequestTimeout((int) acquireTimeout.toMillis())
                        .build())
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .setMaxConnTotal(Integer.MAX_VALUE)
                .setMaxConnPerRoute(Integer.MAX_VALUE)
                .setConnectionTimeToLive(lifeTime.toMillis(), TimeUnit.MILLISECONDS)
                .setRoutePlanner(new DefaultProxyRoutePlanner(new HttpHost(proxy.getHost(), proxy.getPort())) {
                    @Override
                    protected HttpHost determineProxy(
                            HttpHost target,
                            HttpRequest request,
                            HttpContext context) throws HttpException {
                        if (proxy.matches(target.getHostName())) {
                            return super.determineProxy(target, request, context);
                        } else {
                            return null;
                        }
                    }
                })
                .useSystemProperties()
                .disableRedirectHandling()
                .disableAutomaticRetries()
                .evictIdleConnections(idleTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .evictExpiredConnections()
                .addInterceptorFirst(httpRequestInterceptorFirst)
                .addInterceptorLast(httpRequestInterceptorLast)
                .addInterceptorLast(httpResponseInterceptorLast)
                .build();
    }

    @Bean
    public HttpClient reactiveHttpClient() {
        HttpClient httpClient = new HttpClient(new SslContextFactory.Client(true)) {
            @SneakyThrows
            String content(ByteBuffer byteBuffer) {
                return new String(StreamUtils
                        .copyToByteArray(DefaultDataBufferFactory.sharedInstance.wrap(byteBuffer).asInputStream()));
            }

            @Override
            public Request newRequest(URI uri) {
                String elapsedTimeName = "elapsed_time";
                String requestContentName = "request_content";
                String responseContentName = "response_content";
                Request newRequest = super.newRequest(uri);
                newRequest.attribute(elapsedTimeName, System.nanoTime());
                newRequest.onRequestContent((request, content) -> request
                        .attribute(requestContentName, content(content.asReadOnlyBuffer())));
                newRequest.onResponseContent((response, content) -> response.getRequest()
                        .attribute(responseContentName, content(content.asReadOnlyBuffer())));
                newRequest.onComplete(complete -> {
                    long elapsedTime = TimeUnit.NANOSECONDS
                            .toMillis(System.nanoTime() - (long) complete.getRequest()
                                    .getAttributes().get(elapsedTimeName));
                    int statusCode = complete.getResponse().getStatus();
                    boolean error = Optional.ofNullable(HttpStatus.resolve(statusCode))
                            .map(HttpStatus::isError)
                            .orElse(true);
                    Throwable throwable = complete.getFailure();
                    if (isLogEnabled(elapsedTime, error, throwable)) {
                        String requestContent = Optional.ofNullable((String) complete
                                .getRequest().getAttributes().get(requestContentName))
                                .orElseGet(String::new);
                        HttpHeaders requestHeaders = complete.getRequest().getHeaders().stream()
                                .collect(HttpHeaders::new,
                                        (headers, httpField) -> headers.add(httpField.getName(), httpField.getValue()),
                                        HttpHeaders::putAll);
                        String responseContent = Optional.ofNullable((String) complete
                                .getRequest().getAttributes().get(responseContentName))
                                .orElseGet(String::new);
                        HttpHeaders responseHeaders = complete.getResponse().getHeaders().stream()
                                .collect(HttpHeaders::new,
                                        (headers, httpField) -> headers.add(httpField.getName(), httpField.getValue()),
                                        HttpHeaders::putAll);
                        log.info("{},{},{},{},{},{},{},{}",
                                StructuredArguments.keyValue(elapsedTimeName, elapsedTime),
                                StructuredArguments.keyValue("method", complete.getRequest().getMethod()),
                                StructuredArguments.keyValue("requested_uri", complete.getRequest().getURI()),
                                StructuredArguments.keyValue("status_code", statusCode),
                                StructuredArguments.keyValue("request_headers", requestHeaders),
                                StructuredArguments.keyValue("response_headers", responseHeaders),
                                StructuredArguments.keyValue(requestContentName, requestContent),
                                StructuredArguments.keyValue(responseContentName, responseContent),
                                throwable
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
            return this.included.stream().anyMatch(host::equals) && this.excluded.stream().noneMatch(host::equals);
        }
    }
}