package com.f4sitive.account.config;

import ch.qos.logback.access.AccessConstants;
import ch.qos.logback.access.joran.JoranConfigurator;
import ch.qos.logback.access.spi.AccessContext;
import ch.qos.logback.access.spi.AccessEvent;
import ch.qos.logback.access.spi.ServerAdapter;
import ch.qos.logback.access.tomcat.TomcatServerAdapter;
import lombok.SneakyThrows;
import org.apache.catalina.AccessLog;
import org.apache.catalina.Globals;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.AbstractAccessLogValve;
import org.springframework.boot.web.embedded.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
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
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.*;
import java.util.function.BooleanSupplier;

@Configuration(proxyBeanMethods = false)
public class AccessLogConfig {
    @Bean
    FilterRegistrationBean<Filter> contentFilter() {
        FilterRegistrationBean<Filter> filterRegistrationBean = new FilterRegistrationBean<>((request, response, chain) -> {
            BooleanSupplier contentCaching = () -> request.getAttribute(ShallowEtagHeaderFilter.class.getName() + ".STREAMING") == null;
            HttpServletRequest requestToUse = Optional.of((HttpServletRequest) request)
                    .filter(req -> !DispatcherType.ASYNC.equals(request.getDispatcherType()))
                    .filter(req -> !(req instanceof ContentCachingRequestWrapper))
                    .<HttpServletRequest>map(ContentCachingRequestWrapper::new)
                    .orElse((HttpServletRequest) request);
            HttpServletResponse responseToUse = Optional.of((HttpServletResponse) response)
                    .filter(res -> !DispatcherType.ASYNC.equals(request.getDispatcherType()))
                    .filter(res -> !(res instanceof ContentCachingResponseWrapper))
                    .<HttpServletResponse>map(res -> new ContentCachingResponseWrapper(res) {
                        @Override
                        @NonNull
                        public ServletOutputStream getOutputStream() throws IOException {
                            return contentCaching.getAsBoolean() ? super.getOutputStream() : getResponse().getOutputStream();
                        }

                        @Override
                        @NonNull
                        public PrintWriter getWriter() throws IOException {
                            return contentCaching.getAsBoolean() ? super.getWriter() : getResponse().getWriter();
                        }
                    })
                    .orElse((HttpServletResponse) response);
            Optional.ofNullable(requestToUse.getUserPrincipal()).map(Principal::getName).ifPresent(name -> request.setAttribute("org.apache.catalina.AccessLog.RemoteUser", name));
            try {
                chain.doFilter(requestToUse, responseToUse);
            } finally {
                Optional.ofNullable(WebUtils.getNativeRequest(requestToUse, ContentCachingRequestWrapper.class))
                        .filter(req -> !WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted())
                        .ifPresent(req -> request.setAttribute(AccessConstants.LB_INPUT_BUFFER, Optional.ofNullable(request.getAttribute(AccessConstants.LB_INPUT_BUFFER)).filter(byte[].class::isInstance).map(byte[].class::cast).orElseGet(req::getContentAsByteArray)));
                Optional.ofNullable(WebUtils.getNativeResponse(responseToUse, ContentCachingResponseWrapper.class))
                        .filter(res -> !WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted())
                        .filter(res -> contentCaching.getAsBoolean())
                        .ifPresent(res -> {
                            request.setAttribute(AccessConstants.LB_OUTPUT_BUFFER, res.getContentAsByteArray());
                            try {
                                res.copyBodyToResponse();
                            } catch (IOException e) {
                                //ignore
                            }
                        });
            }
        });
        filterRegistrationBean.setDispatcherTypes(DispatcherType.ASYNC, DispatcherType.ERROR, DispatcherType.REQUEST);
        filterRegistrationBean.setName("contentFilter");
        return filterRegistrationBean;
    }

    @SneakyThrows
    @Bean
    WebServerFactoryCustomizer<ConfigurableTomcatWebServerFactory> webServerFactoryCustomizer(Environment environment) {
        AccessContext context = new AccessContext();
        List<String> profiles = new ArrayList<>(Arrays.asList(environment.getActiveProfiles()));
        if (profiles.isEmpty()) {
            profiles.addAll(Arrays.asList(environment.getDefaultProfiles()));
        }
        profiles.forEach(profile -> context.putProperty(profile, profile));
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        configurator.doConfigure(ResourceUtils.getURL("classpath:logback-access-spring.xml"));
        context.start();
        return factory -> factory.addEngineValves(new AbstractAccessLogValve() {
            @Override
            public void log(Request request, Response response, long time) {
                ServerAdapter adapter = serverAdapter(request, response);
                AccessEvent event = accessEvent(request, response, adapter);
                event.setThreadName(Thread.currentThread().getName());
                context.callAppenders(event);
            }

            @Override
            protected void log(CharArrayWriter message) {
                //noting
            }
        });
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
                return new LinkedHashSet<>(response.getHeaderNames()).stream()
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
                            .map(Collections::list)
                            .<Set<String>>map(LinkedHashSet::new)
                            .orElse(Collections.emptySet())
                            .stream()
                            .collect(() -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER),
                                    (map, headerName) -> map.put(
                                            headerName,
                                            String.join(",", Optional.ofNullable(getRequest())
                                                    .map(request -> request.getHeaders(headerName))
                                                    .<List<String>>map(Collections::list)
                                                    .orElse(Collections.emptyList()))
                                    ),
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
                            .map(Collections::list)
                            .<Set<String>>map(LinkedHashSet::new)
                            .orElse(Collections.emptySet())
                            .stream()
                            .collect(LinkedHashMap::new,
                                    (map, parameterName) -> map.put(
                                            parameterName,
                                            Optional.ofNullable(getRequest())
                                                    .map(request -> request.getParameterValues(parameterName))
                                                    .orElse(null)
                                    ),
                                    Map::putAll);
                }
            }
        };
    }
}
