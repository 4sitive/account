package com.f4sitive.account.config;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.config.Customizer;
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
import org.springframework.security.oauth2.server.authorization.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
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
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.lang.reflect.Field;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Configuration(proxyBeanMethods = false)
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConfigurationProperties("authorization-server-security")
public class AuthorizationServerSecurityConfig extends WebSecurityConfigurerAdapter {
    private final ClientRegistrationRepository clientRegistrationRepository;
    @Getter
    @Setter
    private String key = UUID.randomUUID().toString();
    @Getter
    @Setter
    private RSAPrivateKey privateKey;
    @Getter
    @Setter
    private RSAPublicKey publicKey;

    public AuthorizationServerSecurityConfig(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .formLogin(customizer -> customizer.addObjectPostProcessor(new ObjectPostProcessor<Object>() {
                    @Override
                    public Object postProcess(Object object) {
                        if (object instanceof LoginUrlAuthenticationEntryPoint) {
                            String loginFormUrl = ((LoginUrlAuthenticationEntryPoint) object).getLoginFormUrl();
                            return new LoginUrlAuthenticationEntryPoint(loginFormUrl) {
                                @Override
                                protected String determineUrlToUseForThisRequest(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
                                    String queryString = StringUtils.hasText(request.getQueryString()) ? "?" + request.getQueryString() : "";
                                    String registration_hint = ServletRequestUtils.getStringParameter(request, "registration_hint", null);
                                    return Optional.ofNullable(registration_hint)
                                            .filter(StringUtils::hasText)
                                            .map(registrationId -> clientRegistrationRepository.findByRegistrationId(registrationId.toLowerCase()))
                                            .map(registration -> OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI + "/" + registration.getRegistrationId() + queryString)
                                            .orElseGet(() -> super.determineUrlToUseForThisRequest(request, response, exception));
                                }
                            };
                        }
                        return object;
                    }
                }));
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http
                .cors(Customizer.withDefaults())
                .getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .addObjectPostProcessor(new ObjectPostProcessor<Object>() {
                    @Override
                    public Object postProcess(Object object) {
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
                            Field field = ReflectionUtils.findField(OAuth2AuthorizationServerMetadataEndpointFilter.class, "providerSettings");
                            ReflectionUtils.makeAccessible(field);
                            ProviderSettings providerSettings = (ProviderSettings) ReflectionUtils.getField(field, object);
                            RequestMatcher requestMatcher = new AntPathRequestMatcher(OAuth2AuthorizationServerMetadataEndpointFilter.DEFAULT_OAUTH2_AUTHORIZATION_SERVER_METADATA_ENDPOINT_URI, HttpMethod.GET.name());
                            OAuth2AuthorizationServerMetadataHttpMessageConverter authorizationServerMetadataHttpMessageConverter = new OAuth2AuthorizationServerMetadataHttpMessageConverter();
                            return new OAuth2AuthorizationServerMetadataEndpointFilter(providerSettings) {
                                @Override
                                protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
                                    if (!requestMatcher.matches(request)) {
                                        filterChain.doFilter(request, response);
                                        return;
                                    }
                                    OAuth2AuthorizationServerMetadata authorizationServerMetadata = OAuth2AuthorizationServerMetadata.builder()
                                            .issuer(providerSettings.issuer())
                                            .authorizationEndpoint(UriComponentsBuilder.fromUriString(providerSettings.issuer()).path(providerSettings.authorizationEndpoint()).toUriString())
                                            .tokenEndpoint(UriComponentsBuilder.fromUriString(providerSettings.issuer()).path(providerSettings.tokenEndpoint()).toUriString())
                                            .tokenEndpointAuthenticationMethods((authenticationMethods) -> {
                                                authenticationMethods.add("client_secret_basic");
                                                authenticationMethods.add("client_secret_post");
                                            })
                                            .jwkSetUrl(UriComponentsBuilder.fromUriString(providerSettings.issuer()).path(providerSettings.jwkSetEndpoint()).toUriString())
                                            .responseType(OAuth2AuthorizationResponseType.CODE.getValue())
                                            .grantType(AuthorizationGrantType.AUTHORIZATION_CODE.getValue())
                                            .grantType(AuthorizationGrantType.CLIENT_CREDENTIALS.getValue())
                                            .grantType(AuthorizationGrantType.REFRESH_TOKEN.getValue())
                                            .tokenRevocationEndpoint(UriComponentsBuilder.fromUriString(providerSettings.issuer()).path(providerSettings.tokenRevocationEndpoint()).toUriString())
                                            .tokenRevocationEndpointAuthenticationMethods((authenticationMethods) -> {
                                                authenticationMethods.add("client_secret_basic");
                                                authenticationMethods.add("client_secret_post");
                                            })
                                            .tokenIntrospectionEndpoint(UriComponentsBuilder.fromUriString(providerSettings.issuer()).path(providerSettings.tokenIntrospectionEndpoint()).toUriString())
                                            .tokenIntrospectionEndpointAuthenticationMethods((authenticationMethods) -> {
                                                authenticationMethods.add("client_secret_basic");
                                                authenticationMethods.add("client_secret_post");
                                            })
                                            .codeChallengeMethod("plain")
                                            .codeChallengeMethod("S256")
                                            .claim("op_policy_uri", "https://cdn.4sitive.com/policy.html")
                                            .claim("op_tos_uri", "https://cdn.4sitive.com/tos.html")
                                            .build();
                                    ServletServerHttpResponse httpResponse = new ServletServerHttpResponse(response);
                                    authorizationServerMetadataHttpMessageConverter.write(authorizationServerMetadata, MediaType.APPLICATION_JSON, httpResponse);
                                }
                            };
                        }
                        return object;
                    }
                });
    }

    // @formatter:off
    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("4sitive")
                .clientSecret("{noop}secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .redirectUri("http://127.0.0.1/login/oauth2/code/TEST")
                .redirectUri("https://oauth.pstmn.io/v1/callback")
                .redirectUri("positive://login")
                .redirectUri("http://localhost:8080/swagger-ui/oauth2-redirect.html")
                .redirectUri("http://lvh.me:8080/swagger-ui/oauth2-redirect.html")
                .redirectUri("https://api.4sitive.com/swagger-ui/oauth2-redirect.html")
//                .scope(OidcScopes.OPENID)
                .scope("message.read")
                .scope("message.write")
                .tokenSettings(tokenSettings -> tokenSettings.accessTokenTimeToLive(Duration.ofDays(1L)))
//                .clientSettings(clientSettings -> clientSettings.requireUserConsent(true))
                .build();
        return new InMemoryRegisteredClientRepository(registeredClient);
    }
    // @formatter:on

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        List<JWK> keys = new ArrayList<>();
        keys.add(new OctetSequenceKey.Builder(key.getBytes())
                .keyID("enc")
                .build());
        Optional.ofNullable(publicKey)
                .map(RSAKey.Builder::new)
                .map(builder -> Optional.ofNullable(privateKey).map(builder::privateKey).orElse(builder).keyID("sig").build())
                .ifPresent(keys::add);
        JWKSet jwkSet = new JWKSet(keys);
        return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
    }

    @Bean
    public ProviderSettings providerSettings() {
        return new ProviderSettings()
                .authorizationEndpoint("/oauth/authorize")
                .tokenEndpoint("/oauth/token")
                .tokenRevocationEndpoint("/oauth/revoke")
                .tokenIntrospectionEndpoint("/oauth/introspect")
                .jwkSetEndpoint("/oauth/jwks")
                .issuer("https://account.4sitive.com");
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
        return context -> context.getHeaders().jwsAlgorithm(MacAlgorithm.HS256);
    }
}