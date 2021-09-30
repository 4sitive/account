package com.f4sitive.account.config;

import com.f4sitive.account.util.Snowflakes;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthorizationServerMetadata;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType;
import org.springframework.security.oauth2.core.http.converter.OAuth2AuthorizationServerMetadataHttpMessageConverter;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwsEncoder;
import org.springframework.security.oauth2.server.authorization.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2RefreshTokenAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.ProviderSettings;
import org.springframework.security.oauth2.server.authorization.web.OAuth2AuthorizationEndpointFilter;
import org.springframework.security.oauth2.server.authorization.web.OAuth2AuthorizationServerMetadataEndpointFilter;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Configuration(proxyBeanMethods = false)
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConfigurationProperties("authorization-server-security")
public class AuthorizationServerSecurityConfig extends WebSecurityConfigurerAdapter {
    @Setter
    private String key = UUID.randomUUID().toString();
    @Setter
    private String issuer;
    @Setter
    private String opPolicyUri;
    @Setter
    private String opTosUri;
    @Setter
    private String registrationHint;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfigurer<HttpSecurity> authorizationServerConfigurer = new OAuth2AuthorizationServerConfigurer<>();
        authorizationServerConfigurer.addObjectPostProcessor(new ObjectPostProcessor<Object>() {
            @Override
            public Object postProcess(Object object) {
                Snowflakes snowflakes = getApplicationContext().getBean(Snowflakes.class);
                if (object instanceof OAuth2AuthorizationCodeRequestAuthenticationProvider) {
                    ((OAuth2AuthorizationCodeRequestAuthenticationProvider) object).setAuthorizationCodeGenerator(() -> Snowflakes.uuid(snowflakes.generate()).toString());
                }
                if (object instanceof OAuth2AuthorizationCodeAuthenticationProvider) {
                    ((OAuth2AuthorizationCodeAuthenticationProvider) object).setRefreshTokenGenerator(() -> Snowflakes.uuid(snowflakes.generate()).toString());
                }
                if (object instanceof OAuth2RefreshTokenAuthenticationProvider) {
                    ((OAuth2RefreshTokenAuthenticationProvider) object).setRefreshTokenGenerator(() -> Snowflakes.uuid(snowflakes.generate()).toString());
                }
                if (object instanceof OAuth2AuthorizationEndpointFilter) {
                    Field field = ReflectionUtils.findField(OAuth2AuthorizationEndpointFilter.class, "redirectStrategy");
                    ReflectionUtils.makeAccessible(field);
                    ReflectionUtils.setField(field, object, new DefaultRedirectStrategy() {
                        @Override
                        public void sendRedirect(HttpServletRequest request, HttpServletResponse response, String url) throws IOException {
                            Optional.ofNullable(request.getSession(false)).ifPresent(HttpSession::invalidate);
                            SecurityContextHolder.getContext().setAuthentication(null);
                            SecurityContextHolder.clearContext();
                            super.sendRedirect(request, response, url);
                        }
                    });
                }
                if (object instanceof OAuth2AuthorizationServerMetadataEndpointFilter) {
                    return oauth2AuthorizationServerMetadataEndpointFilter();
                }
                return object;
            }
        });
        http
                .formLogin(customizer -> customizer.addObjectPostProcessor(new ObjectPostProcessor<Object>() {
                    @Override
                    public Object postProcess(Object object) {
                        ClientRegistrationRepository clientRegistrationRepository = getApplicationContext().getBean(ClientRegistrationRepository.class);
                        if (object instanceof LoginUrlAuthenticationEntryPoint) {
                            String loginFormUrl = ((LoginUrlAuthenticationEntryPoint) object).getLoginFormUrl();
                            return new LoginUrlAuthenticationEntryPoint(loginFormUrl) {
                                @Override
                                protected String determineUrlToUseForThisRequest(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
                                    return Optional.ofNullable(ServletRequestUtils.getStringParameter(request, "registration_hint", registrationHint))
                                            .filter(StringUtils::hasText)
                                            .map(registrationId -> clientRegistrationRepository.findByRegistrationId(registrationId.toLowerCase()))
                                            .map(registration -> OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI + "/" + registration.getRegistrationId() + Optional.ofNullable(request.getQueryString()).filter(StringUtils::hasText).map("?"::concat).orElse(""))
                                            .orElseGet(() -> super.determineUrlToUseForThisRequest(request, response, exception));
                                }
                            };
                        }
                        return object;
                    }
                }))
                .requestMatcher(request -> authorizationServerConfigurer.getEndpointsMatcher().matches(request) || new AntPathRequestMatcher("/.well-known/oauth-authorization-server/*", HttpMethod.GET.name()).matches(request))
                .authorizeRequests(authorizeRequests ->
                        authorizeRequests.anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers(authorizationServerConfigurer.getEndpointsMatcher()))
                .apply(authorizationServerConfigurer);
    }

    OncePerRequestFilter oauth2AuthorizationServerMetadataEndpointFilter() {
        ProviderSettings providerSettings = getApplicationContext().getBean(ProviderSettings.class);
        RegisteredClientRepository clientRepository = getApplicationContext().getBean(RegisteredClientRepository.class);
        return new OncePerRequestFilter() {
            private final RequestMatcher requestMatcher = new AntPathRequestMatcher("/.well-known/oauth-authorization-server", HttpMethod.GET.name());
            private final RequestMatcher requestMatcherClientId = new AntPathRequestMatcher("/.well-known/oauth-authorization-server/{clientId}", HttpMethod.GET.name());
            private final OAuth2AuthorizationServerMetadataHttpMessageConverter authorizationServerMetadataHttpMessageConverter = new OAuth2AuthorizationServerMetadataHttpMessageConverter();

            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
                if (!requestMatcher.matches(request) && !requestMatcherClientId.matches(request)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                Optional<RegisteredClient> registeredClient = Optional.ofNullable(requestMatcherClientId.matcher(request).getVariables().get("clientId"))
                        .map(clientRepository::findByClientId);
                OAuth2AuthorizationServerMetadata.Builder builder = OAuth2AuthorizationServerMetadata.builder()
                        .issuer(providerSettings.getIssuer())
                        .authorizationEndpoint(UriComponentsBuilder.fromUriString(providerSettings.getIssuer()).path(providerSettings.getAuthorizationEndpoint()).toUriString())
                        .tokenEndpoint(UriComponentsBuilder.fromUriString(providerSettings.getIssuer()).path(providerSettings.getTokenEndpoint()).toUriString())
                        .tokenEndpointAuthenticationMethods((authenticationMethods) -> {
                            authenticationMethods.addAll(registeredClient
                                    .map(RegisteredClient::getClientAuthenticationMethods)
                                    .map(clientAuthenticationMethods -> clientAuthenticationMethods.stream().map(ClientAuthenticationMethod::getValue).collect(Collectors.toList()))
                                    .orElseGet(() -> Arrays.asList(ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue(), ClientAuthenticationMethod.CLIENT_SECRET_POST.getValue())));
                        })
                        .jwkSetUrl(UriComponentsBuilder.fromUriString(providerSettings.getIssuer()).path(providerSettings.getJwkSetEndpoint()).toUriString())
                        .responseType(OAuth2AuthorizationResponseType.CODE.getValue())
                        .grantTypes(grantTypes -> {
                            grantTypes.addAll(registeredClient
                                    .map(RegisteredClient::getAuthorizationGrantTypes)
                                    .map(authorizationGrantTypes -> authorizationGrantTypes.stream().map(AuthorizationGrantType::getValue).collect(Collectors.toList()))
                                    .orElseGet(() -> Arrays.asList(AuthorizationGrantType.AUTHORIZATION_CODE.getValue(), AuthorizationGrantType.CLIENT_CREDENTIALS.getValue(), AuthorizationGrantType.REFRESH_TOKEN.getValue())));
                        })
                        .tokenRevocationEndpoint(UriComponentsBuilder.fromUriString(providerSettings.getIssuer()).path(providerSettings.getTokenRevocationEndpoint()).toUriString())
                        .tokenRevocationEndpointAuthenticationMethods((authenticationMethods) -> {
                            authenticationMethods.addAll(registeredClient
                                    .map(RegisteredClient::getClientAuthenticationMethods)
                                    .map(clientAuthenticationMethods -> clientAuthenticationMethods.stream().map(ClientAuthenticationMethod::getValue).collect(Collectors.toList()))
                                    .orElseGet(() -> Arrays.asList(ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue(), ClientAuthenticationMethod.CLIENT_SECRET_POST.getValue())));
                        })
                        .tokenIntrospectionEndpoint(UriComponentsBuilder.fromUriString(providerSettings.getIssuer()).path(providerSettings.getTokenIntrospectionEndpoint()).toUriString())
                        .tokenIntrospectionEndpointAuthenticationMethods((authenticationMethods) -> {
                            authenticationMethods.addAll(registeredClient
                                    .map(RegisteredClient::getClientAuthenticationMethods)
                                    .map(clientAuthenticationMethods -> clientAuthenticationMethods.stream().map(ClientAuthenticationMethod::getValue).collect(Collectors.toList()))
                                    .orElseGet(() -> Arrays.asList(ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue(), ClientAuthenticationMethod.CLIENT_SECRET_POST.getValue())));
                        })
                        .codeChallengeMethod("plain")
                        .codeChallengeMethod("S256")
                        .claims(claims -> {
                            Optional.ofNullable(opPolicyUri).ifPresent(value -> claims.put("op_policy_uri", value));
                            Optional.ofNullable(opTosUri).ifPresent(value -> claims.put("op_tos_uri", value));
                        });
                ServletServerHttpResponse httpResponse = new ServletServerHttpResponse(response);
                authorizationServerMetadataHttpMessageConverter.write(builder.build(), MediaType.APPLICATION_JSON, httpResponse);
            }
        };
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        JWKSet jwkSet = new JWKSet(new OctetSequenceKey.Builder(key.getBytes()).keyID("enc").build());
        return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
    }

    @Bean
    public ProviderSettings providerSettings() {
        return ProviderSettings.builder()
                .authorizationEndpoint("/oauth/authorize")
                .tokenEndpoint("/oauth/token")
                .tokenRevocationEndpoint("/oauth/revoke")
                .tokenIntrospectionEndpoint("/oauth/introspect")
                .jwkSetEndpoint("/oauth/jwks")
                .issuer(issuer)
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwsEncoder(jwkSource);
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
        return context -> {
            context.getHeaders().algorithm(MacAlgorithm.HS256);
            context.getClaims().claim("ext", context.getAuthorization().<Map>getAttribute("ext"));
        };
    }
}