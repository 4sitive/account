package com.f4sitive.account.config;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.logstash.logback.argument.StructuredArguments;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.apache.hc.client5.http.async.AsyncExecCallback;
import org.apache.hc.client5.http.impl.ChainElement;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.nio.AsyncDataConsumer;
import org.apache.hc.core5.http.nio.CapacityChannel;
import org.apache.hc.core5.http.nio.DataStreamChannel;
import org.apache.hc.core5.http.nio.entity.AsyncEntityProducerWrapper;
import org.apache.hc.core5.http2.ssl.H2ClientTlsStrategy;
import org.apache.hc.core5.util.TimeValue;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.execchain.ClientExecChain;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

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

    String elapsedTimeName = "elapsed_time";
    String requestedUrlName = "requested_url";
    String statusCodeName = "status_code";
    String requestContentName = "request_content";
    String responseContentName = "response_content";
    String requestHeadersName = "request_headers";
    String responseHeadersName = "response_headers";
    String throwableName = "throwable";

    boolean isLogEnabled(long elapsedTime, boolean error, Throwable throwable) {
        return threshold <= elapsedTime || error || throwable != null;
    }

    @SneakyThrows
    @Bean
    CloseableHttpClient httpClient() {
        HttpRequestInterceptor httpRequestInterceptorLast = (request, context) -> {
            context.setAttribute(elapsedTimeName, System.nanoTime());
            context.setAttribute(requestHeadersName, Arrays.stream(request.getAllHeaders())
                    .collect(HttpHeaders::new,
                            (headers, header) -> headers.add(header.getName(), header.getValue()),
                            HttpHeaders::putAll));
            if (request instanceof HttpEntityEnclosingRequest) {
                byte[] content = EntityUtils.toByteArray(((HttpEntityEnclosingRequest) request).getEntity());
                context.setAttribute(requestContentName, content);
            }
        };
        HttpResponseInterceptor httpResponseInterceptorLast = (response, context) -> {
            context.setAttribute(statusCodeName, response.getStatusLine().getStatusCode());
            context.setAttribute(responseHeadersName, Arrays.stream(response.getAllHeaders())
                    .collect(HttpHeaders::new,
                            (headers, header) -> headers.add(header.getName(), header.getValue()),
                            HttpHeaders::putAll));
            if (response.getEntity() != null) {
                byte[] content = EntityUtils.toByteArray(response.getEntity());
                EntityUtils.updateEntity(response, new ByteArrayEntity(content, ContentType.get(response.getEntity())));
                context.setAttribute(responseContentName, content);
            }
        };
        return new HttpClientBuilder() {
            @Override
            protected ClientExecChain decorateProtocolExec(ClientExecChain exec) {
                return (route, request, context, execAware) -> {
                    try {
                        return exec.execute(route, request, context, execAware);
                    } catch (Exception e) {
                        context.setAttribute(throwableName, e);
                        throw e;
                    } finally {
                        long elapsedTime = Optional.ofNullable(context.getAttribute(elapsedTimeName, Long.class))
                                .map(nanoTime -> TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime))
                                .orElse(0L);
                        int statusCode = Optional.ofNullable(context.getAttribute(statusCodeName, Integer.class))
                                .orElse(0);
                        boolean error = Optional.ofNullable(HttpStatus.resolve(statusCode)).map(HttpStatus::isError)
                                .orElse(true);
                        Throwable throwable = Optional.ofNullable(context.getAttribute(throwableName, Throwable.class))
                                .orElse(null);
                        if (isLogEnabled(elapsedTime, error, throwable)) {
                            String requestedUrl = String.valueOf(request.getOriginal());
                            String requestContent = Optional.ofNullable(context
                                    .getAttribute(requestContentName, byte[].class))
                                    .map(String::new)
                                    .orElseGet(String::new);
                            HttpHeaders requestHeaders = Optional.ofNullable(context
                                    .getAttribute(requestHeadersName, HttpHeaders.class))
                                    .orElseGet(HttpHeaders::new);
                            String responseContent = Optional.ofNullable(context
                                    .getAttribute(responseContentName, byte[].class))
                                    .map(String::new)
                                    .orElseGet(String::new);
                            HttpHeaders responseHeaders = Optional.ofNullable(context
                                    .getAttribute(responseHeadersName, HttpHeaders.class))
                                    .orElseGet(HttpHeaders::new);
                            log.info("{},{},{},{},{},{},{}",
                                    StructuredArguments.keyValue(elapsedTimeName, elapsedTime),
                                    StructuredArguments.keyValue(requestedUrlName, requestedUrl),
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
                .addInterceptorLast(httpRequestInterceptorLast)
                .addInterceptorLast(httpResponseInterceptorLast)
                .build();
    }

    @SneakyThrows
    @Bean
    CloseableHttpAsyncClient reactiveHttpClient() {
        return HttpAsyncClientBuilder
                .create()
                .addExecInterceptorBefore(ChainElement.PROTOCOL.name(), "LOG", (request, entityProducer, scope, chain, asyncExecCallback) -> chain.proceed(request, entityProducer, scope, new AsyncExecCallback() {
                    @Override
                    public AsyncDataConsumer handleResponse(
                            org.apache.hc.core5.http.HttpResponse response, EntityDetails entityDetails)
                            throws org.apache.hc.core5.http.HttpException, IOException {
                        return asyncExecCallback.handleResponse(response, entityDetails);
                    }

                    @Override
                    public void handleInformationResponse(
                            org.apache.hc.core5.http.HttpResponse response)
                            throws org.apache.hc.core5.http.HttpException, IOException {
                        asyncExecCallback.handleInformationResponse(response);
                    }

                    @Override
                    public void completed() {
                        log();
                        asyncExecCallback.completed();
                    }

                    @Override
                    public void failed(Exception cause) {
                        scope.clientContext.setAttribute(throwableName, cause);
                        log();
                        asyncExecCallback.failed(cause);
                    }

                    void log() {
                        long elapsedTime = Optional.ofNullable(scope.clientContext.getAttribute(elapsedTimeName, Long.class))
                                .map(nanoTime -> TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime))
                                .orElse(0L);
                        int statusCode = Optional.ofNullable(scope.clientContext.getAttribute(statusCodeName, Integer.class))
                                .orElse(0);
                        boolean error = Optional.ofNullable(HttpStatus.resolve(statusCode)).map(HttpStatus::isError).orElse(true);
                        Throwable throwable = Optional.ofNullable(scope.clientContext.getAttribute(throwableName, Throwable.class))
                                .orElse(null);
                        if (isLogEnabled(elapsedTime, error, throwable)) {
                            String requestedUrl = request + " " + scope.clientContext.getProtocolVersion();
                            String requestContent = Optional.ofNullable(scope.clientContext
                                    .getAttribute(requestContentName, byte[].class))
                                    .map(String::new)
                                    .orElseGet(String::new);
                            HttpHeaders requestHeaders = Optional.ofNullable(scope.clientContext
                                    .getAttribute(requestHeadersName, HttpHeaders.class))
                                    .orElseGet(HttpHeaders::new);
                            String responseContent = Optional.ofNullable(scope.clientContext
                                    .getAttribute(responseContentName, byte[].class))
                                    .map(String::new)
                                    .orElseGet(String::new);
                            HttpHeaders responseHeaders = Optional.ofNullable(scope.clientContext
                                    .getAttribute(responseHeadersName, HttpHeaders.class))
                                    .orElseGet(HttpHeaders::new);
                            log.info("{},{},{},{},{},{},{}",
                                    StructuredArguments.keyValue(elapsedTimeName, elapsedTime),
                                    StructuredArguments.keyValue(requestedUrlName, requestedUrl),
                                    StructuredArguments.keyValue(statusCodeName, statusCode),
                                    StructuredArguments.keyValue(requestHeadersName, requestHeaders),
                                    StructuredArguments.keyValue(responseHeadersName, responseHeaders),
                                    StructuredArguments.keyValue(requestContentName, requestContent),
                                    StructuredArguments.keyValue(responseContentName, responseContent),
                                    throwable
                            );
                        }
                    }
                }))
                .addExecInterceptorLast("CONTENT", (request, entityProducer, scope, chain, asyncExecCallback) -> chain.proceed(request,
                        entityProducer == null ? null : new AsyncEntityProducerWrapper(entityProducer) {
                            @Override
                            public void produce(DataStreamChannel channel) throws IOException {
                                super.produce(new DataStreamChannel() {
                                    @Override
                                    public void requestOutput() {
                                        channel.requestOutput();
                                    }

                                    @Override
                                    public int write(ByteBuffer src) throws IOException {
                                        scope.clientContext.setAttribute(requestContentName, StreamUtils
                                                .copyToByteArray(DefaultDataBufferFactory.sharedInstance
                                                        .wrap(src.asReadOnlyBuffer()).asInputStream()));
                                        return channel.write(src);
                                    }

                                    @Override
                                    public void endStream() throws IOException {
                                        channel.endStream();
                                    }

                                    @Override
                                    public void endStream(List<? extends Header> trailers) throws IOException {
                                        channel.endStream(trailers);
                                    }
                                });
                            }
                        },
                        scope,
                        new AsyncExecCallback() {
                            @Override
                            public AsyncDataConsumer handleResponse(
                                    org.apache.hc.core5.http.HttpResponse response,
                                    EntityDetails entityDetails)
                                    throws IOException, org.apache.hc.core5.http.HttpException {
                                return Optional
                                        .ofNullable(
                                                asyncExecCallback.handleResponse(response, entityDetails))
                                        .map(asyncDataConsumer -> new AsyncDataConsumer() {
                                            @Override
                                            public void releaseResources() {
                                                asyncDataConsumer.releaseResources();
                                            }

                                            @Override
                                            public void updateCapacity(CapacityChannel capacityChannel)
                                                    throws IOException {
                                                asyncDataConsumer.updateCapacity(capacityChannel);
                                            }

                                            @Override
                                            public void consume(ByteBuffer src) throws IOException {
                                                scope.clientContext
                                                        .setAttribute(responseContentName, StreamUtils
                                                                .copyToByteArray(
                                                                        DefaultDataBufferFactory.sharedInstance
                                                                                .wrap(src
                                                                                        .asReadOnlyBuffer())
                                                                                .asInputStream()));
                                                asyncDataConsumer.consume(src);
                                            }

                                            @Override
                                            public void streamEnd(List<? extends Header> trailers)
                                                    throws IOException,
                                                    org.apache.hc.core5.http.HttpException {
                                                asyncDataConsumer.streamEnd(trailers);
                                            }
                                        })
                                        .orElse(null);
                            }

                            @Override
                            public void handleInformationResponse(
                                    org.apache.hc.core5.http.HttpResponse response)
                                    throws IOException, org.apache.hc.core5.http.HttpException {
                                asyncExecCallback.handleInformationResponse(response);
                            }

                            @Override
                            public void completed() {
                                asyncExecCallback.completed();
                            }

                            @Override
                            public void failed(Exception cause) {
                                asyncExecCallback.failed(cause instanceof SocketTimeoutException && !(cause instanceof ConnectTimeoutException) ? new HttpException(cause.getMessage(), cause) : cause);
                            }
                        }
                ))
                .setConnectionManager(PoolingAsyncClientConnectionManagerBuilder.create()
                        .setMaxConnTotal(Integer.MAX_VALUE)
                        .setMaxConnPerRoute(Integer.MAX_VALUE)
                        .setConnectionTimeToLive(TimeValue.ofMilliseconds(lifeTime.toMillis()))
                        .setTlsStrategy(new H2ClientTlsStrategy(SSLContexts.custom()
                                .loadTrustMaterial(TrustAllStrategy.INSTANCE)
                                .build()))
                        .build())
                .setDefaultRequestConfig(org.apache.hc.client5.http.config.RequestConfig
                        .custom()
                        .setConnectTimeout(connectTimeout.toMillis(), TimeUnit.MILLISECONDS)
                        .setResponseTimeout(readTimeout.toMillis(), TimeUnit.MILLISECONDS)
                        .setConnectionRequestTimeout(acquireTimeout.toMillis(), TimeUnit.MILLISECONDS)
                        .build())
                .setRoutePlanner(new org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner(
                        new org.apache.hc.core5.http.HttpHost(proxy.getHost(), proxy.getPort())) {
                    @Override
                    protected org.apache.hc.core5.http.HttpHost determineProxy(
                            org.apache.hc.core5.http.HttpHost target,
                            org.apache.hc.core5.http.protocol.HttpContext context)
                            throws org.apache.hc.core5.http.HttpException {
                        if (proxy.matches(target.getHostName())) {
                            return super.determineProxy(target, context);
                        } else {
                            return null;
                        }
                    }
                })
                .useSystemProperties()
                .disableRedirectHandling()
                .disableAutomaticRetries()
                .evictIdleConnections(TimeValue.ofMilliseconds(idleTimeout.toMillis()))
                .evictExpiredConnections()
                .addRequestInterceptorLast((request, entity, context) -> {
                    context.setAttribute(elapsedTimeName, System.nanoTime());
                    context.setAttribute(requestHeadersName, StreamSupport.stream(
                            Spliterators.spliteratorUnknownSize(request.headerIterator(), Spliterator.ORDERED),
                            false)
                            .collect(HttpHeaders::new,
                                    (headers, header) -> headers.add(header.getName(), header.getValue()),
                                    HttpHeaders::putAll));
                })
                .addResponseInterceptorLast((response, entity, context) -> {
                    context.setAttribute(statusCodeName, response.getCode());
                    context.setAttribute(responseHeadersName, StreamSupport.stream(
                            Spliterators.spliteratorUnknownSize(response.headerIterator(), Spliterator.ORDERED),
                            false)
                            .collect(HttpHeaders::new,
                                    (headers, header) -> headers.add(header.getName(), header.getValue()),
                                    HttpHeaders::putAll));
                })
                .build();
    }

    @Getter
    @Setter
    protected static class Proxy {
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