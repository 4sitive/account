package com.f4sitive.account.config;

import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
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
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.ProviderSettings;
import org.springframework.security.oauth2.server.authorization.web.OAuth2AuthorizationEndpointFilter;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.ServletRequestUtils;

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
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .addObjectPostProcessor(new ObjectPostProcessor<Object>() {
                    @Override
                    public Object postProcess(Object object) {
                        if (object instanceof OAuth2AuthorizationEndpointFilter) {
                            Field field = ReflectionUtils.findField(OAuth2AuthorizationEndpointFilter.class, "redirectStrategy");
                            ReflectionUtils.makeAccessible(field);
                            ReflectionUtils.setField(field, object, new DefaultRedirectStrategy() {
                                @SneakyThrows
                                @Override
                                public void sendRedirect(HttpServletRequest request, HttpServletResponse response, String url) throws IOException {
                                    Optional.ofNullable(request.getSession(false)).ifPresent(HttpSession::invalidate);
                                    SecurityContextHolder.getContext().setAuthentication(null);
                                    SecurityContextHolder.clearContext();
                                    super.sendRedirect(request, response, url);
                                }
                            });
                        }
                        return object;
                    }
                });
        http.formLogin(customizer -> customizer.addObjectPostProcessor(new ObjectPostProcessor<Object>() {
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
                                    .map(registrationId -> clientRegistrationRepository.findByRegistrationId(registrationId.toUpperCase()))
                                    .map(registration -> OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI + "/" + registration.getRegistrationId() + queryString)
                                    .orElseGet(() -> super.determineUrlToUseForThisRequest(request, response, exception));
                        }
                    };
                }
                return object;
            }
        }));
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