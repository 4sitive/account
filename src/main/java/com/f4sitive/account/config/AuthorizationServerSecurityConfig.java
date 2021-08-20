package com.f4sitive.account.config;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.Getter;
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
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
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
        OAuth2AuthorizationServerConfigurer<HttpSecurity> authorizationServerConfigurer = new OAuth2AuthorizationServerConfigurer<>();
//        authorizationServerConfigurer.con
//        authorizationServerConfigurer.authorizationEndpoint(c -> c.)
        authorizationServerConfigurer.addObjectPostProcessor(new ObjectPostProcessor<Object>() {
            @Override
            public Object postProcess(Object object) {
                if (object instanceof OAuth2AuthorizationEndpointFilter) {
                    return oAuth2AuthorizationEndpointFilter((OAuth2AuthorizationEndpointFilter) object);
                }
                if (object instanceof OAuth2AuthorizationServerMetadataEndpointFilter) {
                    return oAuth2AuthorizationServerMetadataEndpointFilter((OAuth2AuthorizationServerMetadataEndpointFilter) object);
                }
                return object;
            }
        });
        http
                .requestMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .authorizeRequests(authorizeRequests ->
                        authorizeRequests.anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers(authorizationServerConfigurer.getEndpointsMatcher()))
                .apply(authorizationServerConfigurer);
    }

    OAuth2AuthorizationEndpointFilter oAuth2AuthorizationEndpointFilter(OAuth2AuthorizationEndpointFilter object) {
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
        return object;
    }

    OncePerRequestFilter oAuth2AuthorizationServerMetadataEndpointFilter(OAuth2AuthorizationServerMetadataEndpointFilter object) {
        Field field = ReflectionUtils.findField(OAuth2AuthorizationServerMetadataEndpointFilter.class, "providerSettings");
        ReflectionUtils.makeAccessible(field);
        ProviderSettings providerSettings = (ProviderSettings) ReflectionUtils.getField(field, object);
        return new OncePerRequestFilter() {
            private RequestMatcher requestMatcher = new AntPathRequestMatcher("/.well-known/oauth-authorization-server", HttpMethod.GET.name());
            private OAuth2AuthorizationServerMetadataHttpMessageConverter authorizationServerMetadataHttpMessageConverter = new OAuth2AuthorizationServerMetadataHttpMessageConverter();

            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
                if (!requestMatcher.matches(request)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                OAuth2AuthorizationServerMetadata authorizationServerMetadata = OAuth2AuthorizationServerMetadata.builder()
                        .issuer(providerSettings.getIssuer())
                        .authorizationEndpoint(UriComponentsBuilder.fromUriString(providerSettings.getIssuer()).path(providerSettings.getAuthorizationEndpoint()).toUriString())
                        .tokenEndpoint(UriComponentsBuilder.fromUriString(providerSettings.getIssuer()).path(providerSettings.getTokenEndpoint()).toUriString())
                        .tokenEndpointAuthenticationMethods((authenticationMethods) -> {
                            authenticationMethods.add(ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue());
                            authenticationMethods.add(ClientAuthenticationMethod.CLIENT_SECRET_POST.getValue());
                        })
                        .jwkSetUrl(UriComponentsBuilder.fromUriString(providerSettings.getIssuer()).path(providerSettings.getJwkSetEndpoint()).toUriString())
                        .responseType(OAuth2AuthorizationResponseType.CODE.getValue())
                        .grantType(AuthorizationGrantType.AUTHORIZATION_CODE.getValue())
                        .grantType(AuthorizationGrantType.CLIENT_CREDENTIALS.getValue())
                        .grantType(AuthorizationGrantType.REFRESH_TOKEN.getValue())
                        .tokenRevocationEndpoint(UriComponentsBuilder.fromUriString(providerSettings.getIssuer()).path(providerSettings.getTokenRevocationEndpoint()).toUriString())
                        .tokenRevocationEndpointAuthenticationMethods((authenticationMethods) -> {
                            authenticationMethods.add(ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue());
                            authenticationMethods.add(ClientAuthenticationMethod.CLIENT_SECRET_POST.getValue());
                        })
                        .tokenIntrospectionEndpoint(UriComponentsBuilder.fromUriString(providerSettings.getIssuer()).path(providerSettings.getTokenIntrospectionEndpoint()).toUriString())
                        .tokenIntrospectionEndpointAuthenticationMethods((authenticationMethods) -> {
                            authenticationMethods.add(ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue());
                            authenticationMethods.add(ClientAuthenticationMethod.CLIENT_SECRET_POST.getValue());
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
        return ProviderSettings.builder()
                .authorizationEndpoint("/oauth/authorize")
                .tokenEndpoint("/oauth/token")
                .tokenRevocationEndpoint("/oauth/revoke")
                .tokenIntrospectionEndpoint("/oauth/introspect")
                .jwkSetEndpoint("/oauth/jwks")
                .issuer("https://account.4sitive.com")
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
            context.getClaims().claim("ext", Collections.singletonMap("usr", UUID.randomUUID().toString()));
        };
    }
}