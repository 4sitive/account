package com.f4sitive.account.config;

import ch.qos.logback.access.AccessConstants;
import ch.qos.logback.access.joran.JoranConfigurator;
import ch.qos.logback.access.spi.AccessContext;
import ch.qos.logback.access.spi.AccessEvent;
import ch.qos.logback.access.spi.ServerAdapter;
import ch.qos.logback.access.tomcat.TomcatServerAdapter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.AccessLog;
import org.apache.catalina.Globals;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.AccessLogValve;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.web.embedded.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.NonNull;
import org.springframework.util.ResourceUtils;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
@ManagementContextConfiguration(proxyBeanMethods = false)
public class AccessLogConfig {
    @Bean
    FilterRegistrationBean<Filter> contentFilter() {
        FilterRegistrationBean<Filter> filterRegistrationBean = new FilterRegistrationBean<>((request, response, chain) -> {
            HttpServletRequest requestToUse = (HttpServletRequest) request;
            Optional.ofNullable(requestToUse.getUserPrincipal()).map(Principal::getName).ifPresent(remoteUser -> request.setAttribute("org.apache.catalina.AccessLog.RemoteUser", remoteUser));
            HttpServletResponse responseToUse = (HttpServletResponse) response;
            if (!DispatcherType.ASYNC.equals(request.getDispatcherType())) {
                if (!(request instanceof ContentCachingRequestWrapper)) {
                    requestToUse = new ContentCachingRequestWrapper(requestToUse);
                }
                if (!(response instanceof ContentCachingResponseWrapper)) {
                    responseToUse = new ContentCachingResponseWrapper(responseToUse) {
                        @Override
                        @NonNull
                        public ServletOutputStream getOutputStream() throws IOException {
                            return request.getAttribute(ShallowEtagHeaderFilter.class.getName() + ".STREAMING") == null ? super.getOutputStream() : getResponse().getOutputStream();
                        }

                        @Override
                        @NonNull
                        public PrintWriter getWriter() throws IOException {
                            return request.getAttribute(ShallowEtagHeaderFilter.class.getName() + ".STREAMING") == null ? super.getWriter() : getResponse().getWriter();
                        }
                    };
                }
            }
            try {
                chain.doFilter(requestToUse, responseToUse);
            } finally {
                if (WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted()) {
                    byte[] content = request.getAttribute(AccessConstants.LB_INPUT_BUFFER) instanceof byte[] ? (byte[]) requestToUse.getAttribute(AccessConstants.LB_INPUT_BUFFER) : new byte[0];
                    request.setAttribute(AccessConstants.LB_INPUT_BUFFER, content);
                } else {
                    ContentCachingRequestWrapper contentCachingRequest = WebUtils.getNativeRequest(requestToUse, ContentCachingRequestWrapper.class);
                    if (contentCachingRequest != null) {
                        byte[] content = request.getAttribute(AccessConstants.LB_INPUT_BUFFER) instanceof byte[] ? (byte[]) request.getAttribute(AccessConstants.LB_INPUT_BUFFER) : contentCachingRequest.getContentAsByteArray();
                        request.setAttribute(AccessConstants.LB_INPUT_BUFFER, content);
                    }
                    ContentCachingResponseWrapper contentCachingResponse = WebUtils.getNativeResponse(responseToUse, ContentCachingResponseWrapper.class);
                    if (contentCachingResponse != null && request.getAttribute(ShallowEtagHeaderFilter.class.getName() + ".STREAMING") == null) {
                        request.setAttribute(AccessConstants.LB_OUTPUT_BUFFER, contentCachingResponse.getContentAsByteArray());
                        contentCachingResponse.copyBodyToResponse();
                    }
                }
            }
        });
        filterRegistrationBean.setDispatcherTypes(DispatcherType.ASYNC, DispatcherType.ERROR, DispatcherType.REQUEST);
        filterRegistrationBean.setName("contentFilter");
        return filterRegistrationBean;
    }

    @SneakyThrows
    @Bean(initMethod = "start", destroyMethod = "stop")
    AccessContext accessContext(ApplicationContext applicationContext) {
        AccessContext context = new AccessContext();
        Arrays.stream(applicationContext.getEnvironment().getActiveProfiles())
                .forEach(profile -> context.putProperty(profile, profile));
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        configurator.doConfigure(ResourceUtils.getURL("classpath:logback-access-spring.xml"));
        return context;
    }

    ServerAdapter serverAdapter(Request request, Response response) {
        return new TomcatServerAdapter(request, response) {
            @Override
            public long getContentLength() {
                long length = response.getBytesWritten(false);
                if (length <= 0) {
                    Object start = request.getAttribute(Globals.SENDFILE_FILE_START_ATTR);
                    if (start instanceof Long) {
                        Object end = request.getAttribute(Globals.SENDFILE_FILE_END_ATTR);
                        if (end instanceof Long) {
                            length = (Long) end - (Long) start;
                        }
                    }
                }
                return length <= 0 ? super.getContentLength() : length;
            }

            @Override
            public Map<String, String> buildResponseHeaderMap() {
                return new LinkedHashSet<>(response.getHeaderNames())
                        .stream()
                        .collect(LinkedHashMap::new,
                                (map, headerName) -> map.put(headerName, String.join(",", response.getHeaders(headerName))),
                                Map::putAll);
            }
        };
    }

    AccessEvent accessEvent(Request request, Response response, ServerAdapter adapter) {
        return new AccessEvent(request, response, adapter) {
            private String remoteHost;
            private String protocol;
            private String remoteUser;
            private String requestContent;
            private Map<String, String> requestHeaderMap;
            private Map<String, String[]> requestParameterMap;

            @Override
            public String getRemoteHost() {
                return Optional.ofNullable(remoteHost)
                        .orElseGet(() -> remoteHost = Optional.ofNullable(getRequest())
                                .map(request -> (String) request.getAttribute(AccessLog.REMOTE_HOST_ATTRIBUTE))
                                .orElseGet(super::getRemoteHost));
            }

            @Override
            public String getProtocol() {
                return Optional.ofNullable(protocol)
                        .orElseGet(() -> protocol = Optional.ofNullable(getRequest())
                                .map(request -> (String) request.getAttribute(AccessLog.PROTOCOL_ATTRIBUTE))
                                .orElseGet(super::getProtocol));
            }

            @Override
            public String getRemoteUser() {
                return Optional.ofNullable(remoteUser)
                        .orElseGet(() -> remoteUser = Optional.ofNullable(getRequest())
                                .map(request -> (String) request.getAttribute("org.apache.catalina.AccessLog.RemoteUser"))
                                .orElseGet(super::getRemoteUser));
            }

            @Override
            public String getRequestContent() {
                return Optional.ofNullable(requestContent)
                        .orElseGet(() -> requestContent = Optional.ofNullable(getRequest())
                                .map(request -> (byte[]) request.getAttribute(AccessConstants.LB_INPUT_BUFFER))
                                .map(String::new)
                                .orElseGet(super::getRequestContent));
            }

            @Override
            public String getRequestHeader(String key) {
                buildRequestHeaderMap();
                return Optional.ofNullable(requestHeaderMap.get(key.toLowerCase()))
                        .orElse("-");
            }

            @Override
            public Map<String, String> getRequestHeaderMap() {
                buildRequestHeaderMap();
                return requestHeaderMap;
            }

            @Override
            public void buildRequestHeaderMap() {
                if (requestHeaderMap == null) {
                    requestHeaderMap = Optional.ofNullable(getRequest())
                            .map(HttpServletRequest::getHeaderNames)
                            .map(headerNames -> Collections.list(headerNames).stream())
                            .orElse(Stream.empty())
                            .collect(() -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER),
                                    (map, headerName) -> map.put(headerName, String.join(",", Optional.ofNullable(getRequest())
                                            .map(request -> request.getHeaders(headerName))
                                            .map(Collections::list)
                                            .orElseGet(ArrayList::new))),
                                    Map::putAll);
                }
            }

            @Override
            public Map<String, String[]> getRequestParameterMap() {
                buildRequestParameterMap();
                return requestParameterMap;
            }

            @Override
            public String[] getRequestParameter(String key) {
                buildRequestParameterMap();
                return Optional.ofNullable(requestParameterMap.get(key))
                        .orElseGet(() -> new String[]{"-"});
            }

            @Override
            public void buildRequestParameterMap() {
                if (requestParameterMap == null) {
                    requestParameterMap = Optional.ofNullable(getRequest())
                            .map(HttpServletRequest::getParameterNames)
                            .map(parameterName -> Collections.list(parameterName).stream())
                            .orElse(Stream.empty())
                            .collect(LinkedHashMap::new,
                                    (map, parameterName) -> map.put(parameterName, Optional.ofNullable(getRequest())
                                            .map(request -> request.getParameterValues(parameterName))
                                            .orElse(new String[0])),
                                    Map::putAll);
                }
            }
        };
    }

    @Bean
    WebServerFactoryCustomizer<ConfigurableTomcatWebServerFactory> webServerFactoryCustomizer(AccessContext accessContext) {
        return factory -> factory.addEngineValves(new AccessLogValve() {
            @Override
            public void log(Request request, Response response, long time) {
                ServerAdapter adapter = serverAdapter(request, response);
                AccessEvent event = accessEvent(request, response, adapter);
                event.setThreadName(Thread.currentThread().getName());
                accessContext.callAppenders(event);
            }
        });
    }
}
