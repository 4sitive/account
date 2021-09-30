package com.f4sitive.account.service;

import com.f4sitive.account.entity.AuthorizedClient;
import com.f4sitive.account.repository.AuthorizedClientRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.RequestEntity;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.DelegatingOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.JdbcOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.RefreshTokenOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.endpoint.DefaultRefreshTokenTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequestEntityConverter;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuthorizedClientService extends JdbcOAuth2AuthorizedClientService implements OAuth2AuthorizedClientRepository {
    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final AuthorizedClientRepository authorizedClientRepository;
    private final ClientRegistrationRepository registrationRepository;

    public AuthorizedClientService(JdbcOperations jdbcOperations,
                                   ClientRegistrationRepository registrationRepository,
                                   OAuth2UserService userService,
                                   RestTemplateBuilder restTemplateBuilder,
                                   MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter,
                                   AuthorizedClientRepository authorizedClientRepository) {
        super(jdbcOperations, registrationRepository, new DefaultLobHandler());
        this.authorizedClientRepository = authorizedClientRepository;
        this.registrationRepository = registrationRepository;
        RestTemplate restTemplate = restTemplateBuilder
                .errorHandler(new OAuth2ErrorResponseErrorHandler())
                .messageConverters(new FormHttpMessageConverter(), new OAuth2AccessTokenResponseHttpMessageConverter() {
                    @Override
                    protected OAuth2AccessTokenResponse readInternal(Class<? extends OAuth2AccessTokenResponse> clazz, HttpInputMessage inputMessage) throws HttpMessageNotReadableException {
                        try {
                            return super.readInternal(clazz, inputMessage);
                        } catch (HttpMessageNotReadableException e) {
                            if (e.getCause() instanceof IllegalArgumentException) {
                                log.error(e.getMessage(), e);
                                return null;
                            }
                            throw e;
                        }
                    }
                }, mappingJackson2HttpMessageConverter)
                .build();

        DefaultRefreshTokenTokenResponseClient refreshTokenTokenResponseClient = new DefaultRefreshTokenTokenResponseClient();
        refreshTokenTokenResponseClient.setRestOperations(restTemplate);
        refreshTokenTokenResponseClient.setRequestEntityConverter(oauth2RefreshTokenGrantRequestEntityConverter());

        RefreshTokenOAuth2AuthorizedClientProvider refreshTokenOAuth2AuthorizedClientProvider = new RefreshTokenOAuth2AuthorizedClientProvider();
        refreshTokenOAuth2AuthorizedClientProvider.setAccessTokenResponseClient(refreshTokenTokenResponseClient);

        List<OAuth2AuthorizedClientProvider> authorizedClientProviders = new ArrayList<>();
        authorizedClientProviders.add(context -> {
            if (context.getAuthorizedClient() == null) {
                return null;
            }
            Instant expiresAt = Instant.now().minus(10L, ChronoUnit.MINUTES);
            try {
                return Optional.ofNullable(refreshTokenOAuth2AuthorizedClientProvider.authorize(Optional.ofNullable(context.<String>getAttribute("refresh"))
                                .filter("true"::equals)
                                .map(refresh -> OAuth2AuthorizationContext
                                        .withAuthorizedClient(new OAuth2AuthorizedClient(context.getClientRegistration(),
                                                context.getAuthorizedClient().getPrincipalName(),
                                                new OAuth2AccessToken(context.getAuthorizedClient().getAccessToken().getTokenType(),
                                                        context.getAuthorizedClient().getAccessToken().getTokenValue(),
                                                        expiresAt.minus(10L, ChronoUnit.MINUTES),
                                                        expiresAt,
                                                        context.getAuthorizedClient().getAccessToken().getScopes()),
                                                context.getAuthorizedClient().getRefreshToken()))
                                        .principal(context.getPrincipal())
                                        .attributes(attributes -> attributes.putAll(context.getAttributes()))
                                        .build())
                                .orElse(context)))
                        .map(authorizedClient -> new OAuth2AuthorizedClient(authorizedClient.getClientRegistration(),
                                context.getAuthorizedClient().getPrincipalName(),
                                authorizedClient.getAccessToken(),
                                authorizedClient.getRefreshToken()))
                        .orElse(null);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return null;
            }
        });

        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrationRepository, this);
        authorizedClientManager.setContextAttributesMapper(new AuthorizedClientServiceOAuth2AuthorizedClientManager.DefaultContextAttributesMapper() {
            @Override
            public Map<String, Object> apply(OAuth2AuthorizeRequest authorizeRequest) {
                Map<String, Object> contextAttributes = new HashMap<>(super.apply(authorizeRequest));
                contextAttributes.putAll(authorizeRequest.getAttributes());
                return contextAttributes;
            }
        });
        authorizedClientManager.setAuthorizedClientProvider(new DelegatingOAuth2AuthorizedClientProvider(authorizedClientProviders));
        authorizedClientManager.setAuthorizationSuccessHandler((authorizedClient, principal, attributes) -> {
            OAuth2User oauth2User = userService.loadUser(new OAuth2UserRequest(authorizedClient.getClientRegistration(), authorizedClient.getAccessToken(), attributes));
            Authentication authentication = new AbstractAuthenticationToken(null) {
                @Override
                public Object getCredentials() {
                    return "";
                }

                @Override
                public Object getPrincipal() {
                    return oauth2User;
                }
            };
            saveAuthorizedClient(authorizedClient, authentication);
        });
        this.authorizedClientManager = authorizedClientManager;
    }

    OAuth2RefreshTokenGrantRequestEntityConverter oauth2RefreshTokenGrantRequestEntityConverter() {
        return new OAuth2RefreshTokenGrantRequestEntityConverter() {
            @Override
            public RequestEntity<?> convert(OAuth2RefreshTokenGrantRequest refreshTokenGrantRequest) {
                RequestEntity<?> convert = super.convert(refreshTokenGrantRequest);
                return new RequestEntity<>(convert.getBody(), HttpHeaders.writableHttpHeaders(convert.getHeaders()), convert.getMethod(), convert.getUrl());
            }
        };
    }

    public OAuth2AuthorizedClient authorize(String clientRegistrationId, String userId, Map<String, Object> attributes) {
        return Optional.ofNullable(loadAuthorizedClient(clientRegistrationId, userId))
                .map(authorizedClient -> OAuth2AuthorizeRequest.withAuthorizedClient(authorizedClient)
                        .principal(userId)
                        .attributes(consumer -> consumer.putAll(attributes))
                        .build()
                )
                .map(authorizedClientManager::authorize)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<OAuth2AuthorizedClient> findAllByUserId(String userId) {
        return authorizedClientRepository.queryAllByUserId(userId)
                .stream()
                .map(this::oauth2AuthorizedClient)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public OAuth2AuthorizedClient loadAuthorizedClient(String registrationId, String userId) {
        return authorizedClientRepository.findById(new AuthorizedClient.ID(userId, registrationId))
                .map(this::oauth2AuthorizedClient)
                .orElse(null);
    }

    OAuth2AuthorizedClient oauth2AuthorizedClient(AuthorizedClient authorizedClient) {
        Instant issuedAt = Optional.ofNullable(authorizedClient.getAccessTokenIssuedAt())
                .orElseGet(() -> Instant.now().minus(10L, ChronoUnit.HOURS));
        Instant expiresAt = Optional.ofNullable(authorizedClient.getAccessTokenExpiresAt())
                .filter(accessTokenExpiresAt -> accessTokenExpiresAt.isAfter(issuedAt))
                .orElseGet(() -> issuedAt.plus(10L, ChronoUnit.MINUTES));
        String accessTokenValue = authorizedClient.getAccessToken();
        String refreshTokenValue = authorizedClient.getRefreshToken();
        Set<String> scopes = authorizedClient.getAccessTokenScopes();
        OAuth2AccessToken accessToken = Optional.ofNullable(accessTokenValue)
                .filter(StringUtils::hasText)
                .map(token -> new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, token, issuedAt, expiresAt, scopes))
                .orElseGet(() -> new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "NONE", issuedAt, expiresAt, scopes));
        OAuth2RefreshToken refreshToken = Optional.ofNullable(refreshTokenValue)
                .filter(StringUtils::hasText)
                .map(token -> new OAuth2RefreshToken(token, authorizedClient.getRefreshTokenIssuedAt()))
                .orElse(null);
        return new OAuth2AuthorizedClient(
                registrationRepository.findByRegistrationId(authorizedClient.getId().getRegistrationId()),
                Optional.ofNullable(authorizedClient.getName()).orElse("NONE"),
                accessToken,
                refreshToken);
    }

    @Transactional
    @Override
    public void saveAuthorizedClient(OAuth2AuthorizedClient oauth2AuthorizedClient, Authentication principal) {
        String registrationId = oauth2AuthorizedClient.getClientRegistration().getRegistrationId();
        if (Clock.systemUTC().instant().isAfter(oauth2AuthorizedClient.getAccessToken().getExpiresAt())) {
            return;
        }
        String userId = principal.getName();
        AuthorizedClient.ID id = new AuthorizedClient.ID(userId, registrationId);
        AuthorizedClient authorizedClient = authorizedClientRepository.findById(id)
                .orElseGet(() -> new AuthorizedClient(id));
        authorizedClient.setAccessToken(oauth2AuthorizedClient.getAccessToken().getTokenValue());
        authorizedClient.setAccessTokenIssuedAt(oauth2AuthorizedClient.getAccessToken().getIssuedAt());
        authorizedClient.setAccessTokenExpiresAt(oauth2AuthorizedClient.getAccessToken().getExpiresAt());
        authorizedClient.setAccessTokenScopes(oauth2AuthorizedClient.getAccessToken().getScopes());
        Optional.ofNullable(oauth2AuthorizedClient.getRefreshToken())
                .map(OAuth2RefreshToken::getTokenValue)
                .ifPresent(authorizedClient::setRefreshToken);
        Optional.ofNullable(oauth2AuthorizedClient.getRefreshToken())
                .map(OAuth2RefreshToken::getIssuedAt)
                .ifPresent(authorizedClient::setRefreshTokenIssuedAt);
        Optional.ofNullable(principal.getPrincipal())
                .filter(OAuth2AuthenticatedPrincipal.class::isInstance)
                .map(OAuth2AuthenticatedPrincipal.class::cast)
                .ifPresent(oauth2AuthenticatedPrincipal -> {
                    authorizedClient.setAttributes(oauth2AuthenticatedPrincipal.getAttributes());
                    Optional.ofNullable(oauth2AuthenticatedPrincipal.<String>getAttribute("user_name")).ifPresent(authorizedClient::setName);
                });
        authorizedClientRepository.save(authorizedClient);
    }

    @Transactional
    @Override
    public void removeAuthorizedClient(String registrationId, String userId) {
        authorizedClientRepository.findById(new AuthorizedClient.ID(userId, registrationId))
                .ifPresent(authorizedClientRepository::delete);
    }

    @Override
    public OAuth2AuthorizedClient loadAuthorizedClient(String clientRegistrationId, Authentication principal, HttpServletRequest request) {
        return loadAuthorizedClient(clientRegistrationId, principal.getName());
    }

    @Override
    public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal, HttpServletRequest request, HttpServletResponse response) {
        saveAuthorizedClient(authorizedClient, principal);
    }

    @Override
    public void removeAuthorizedClient(String clientRegistrationId, Authentication principal, HttpServletRequest request, HttpServletResponse response) {
        removeAuthorizedClient(clientRegistrationId, principal.getName());
    }
}
