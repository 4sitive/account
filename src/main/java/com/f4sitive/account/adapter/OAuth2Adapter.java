package com.f4sitive.account.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.DefaultRefreshTokenTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequestEntityConverter;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequestEntityConverter;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
public class OAuth2Adapter {
    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final RestTemplate restTemplate;
    private final RestTemplate lbRestTemplate;
    private final OAuth2UserRequestEntityConverter requestEntityConverter;
    private final ClientRegistrationRepository clientRegistrationRepository;

    public OAuth2Adapter(OAuth2AuthorizedClientService authorizedClientService,
                         ClientRegistrationRepository clientRegistrationRepository,
                         RestTemplateBuilder restTemplateBuilder,
                         OAuth2RefreshTokenGrantRequestEntityConverter oAuth2RefreshTokenGrantRequestEntityConverter,
                         OAuth2UserRequestEntityConverter requestEntityConverter,
                         MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        restTemplateBuilder = restTemplateBuilder
                .errorHandler(new DefaultResponseErrorHandler() {
                    @Override
                    public boolean hasError(ClientHttpResponse response) {
                        return false;
                    }
                });
        this.restTemplate = restTemplateBuilder.build();
        this.lbRestTemplate = restTemplateBuilder.build();
        this.requestEntityConverter = requestEntityConverter;

        RestTemplate restTemplate = restTemplateBuilder
                .errorHandler(new OAuth2ErrorResponseErrorHandler())
                .messageConverters(new FormHttpMessageConverter(), new OAuth2AccessTokenResponseHttpMessageConverter() {
                    @Override
                    protected OAuth2AccessTokenResponse readInternal(Class<? extends OAuth2AccessTokenResponse> clazz, HttpInputMessage inputMessage) throws HttpMessageNotReadableException {
                        try {
                            return super.readInternal(clazz, inputMessage);
                        } catch (HttpMessageNotReadableException e) {
                            if (e.getCause() instanceof IllegalArgumentException) {
                                log.error("OAuth2AccessTokenResponseHttpMessageConverter", e);
                                return null;
                            }
                            throw e;
                        }
                    }
                }, mappingJackson2HttpMessageConverter)
                .build();

        DefaultRefreshTokenTokenResponseClient refreshTokenTokenResponseClient = new DefaultRefreshTokenTokenResponseClient();
        refreshTokenTokenResponseClient.setRestOperations(restTemplate);
        refreshTokenTokenResponseClient.setRequestEntityConverter(oAuth2RefreshTokenGrantRequestEntityConverter);

        List<OAuth2AuthorizedClientProvider> authorizedClientProviders = new ArrayList<>();
        OAuth2AuthorizedClientProviderBuilder.builder()
                .refreshToken(configurer -> {
                    OAuth2AuthorizedClientProvider authorizedClientProvider = configurer
                            .accessTokenResponseClient(refreshTokenTokenResponseClient)
                            .build();
                    authorizedClientProviders.add(context -> {
                        if (context.getAuthorizedClient() == null) {
                            return null;
                        }
                        Instant expiresAt = Instant.now().minus(10L, ChronoUnit.MINUTES);
                        try {
                            return Optional.ofNullable(authorizedClientProvider.authorize(Optional.ofNullable(context.<String>getAttribute("refresh"))
                                    .filter("true"::equals)
                                    .map(refresh -> OAuth2AuthorizationContext
                                            .withAuthorizedClient(new OAuth2AuthorizedClient(context.getClientRegistration(),
                                                    context.getAuthorizedClient().getPrincipalName(),
                                                    new OAuth2AccessToken(context.getAuthorizedClient().getAccessToken().getTokenType(),
                                                            context.getAuthorizedClient().getAccessToken().getTokenValue(),
                                                            expiresAt.minus(10L, ChronoUnit.MINUTES),
                                                            expiresAt),
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
                            log.error("RefreshTokenOAuth2AuthorizedClientProvider", e);
                            return null;
                        }
                    });
                });
        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientService);
        authorizedClientManager.setAuthorizedClientProvider(new DelegatingOAuth2AuthorizedClientProvider(authorizedClientProviders));
        authorizedClientManager.setContextAttributesMapper(authorizeRequest -> {
            Map<String, Object> contextAttributes = new HashMap<>(authorizeRequest.getAttributes());
            String scope = authorizeRequest.getAttribute(OAuth2ParameterNames.SCOPE);
            if (StringUtils.hasText(scope)) {
                contextAttributes.put(OAuth2AuthorizationContext.REQUEST_SCOPE_ATTRIBUTE_NAME, StringUtils.delimitedListToStringArray(scope, " "));
            }
            return contextAttributes;
        });
        this.authorizedClientManager = authorizedClientManager;
    }

    @Bean
    @LoadBalanced
    public RestTemplate userInfoRestTemplate() {
        return lbRestTemplate;
    }


    public OAuth2AuthorizedClient authorize(String registrationId, String principalName, Map<String, Object> params) {
        Authentication authentication = new OAuth2AuthenticationToken(new OAuth2User() {
            @Override
            public String getName() {
                return principalName;
            }

            @Override
            public Map<String, Object> getAttributes() {
                return Collections.emptyMap();
            }

            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return AuthorityUtils.NO_AUTHORITIES;
            }
        }, AuthorityUtils.NO_AUTHORITIES, registrationId);

        return authorizedClientManager.authorize(OAuth2AuthorizeRequest.withClientRegistrationId(registrationId)
                .principal(authentication)
                .attributes(consumer -> consumer.putAll(params))
                .build());
    }

    public ResponseEntity<JsonNode> info(OAuth2AuthorizedClient authorizedClient, Map<String, Object> additionalParameters) {
        ClientRegistration clientRegistration = authorizedClient.getClientRegistration();
        OAuth2UserRequest oAuth2UserRequest = new OAuth2UserRequest(clientRegistration, authorizedClient.getAccessToken(), additionalParameters);
        RequestEntity<?> requestEntity = this.requestEntityConverter.convert(oAuth2UserRequest);
        String registrationId = clientRegistration.getRegistrationId();
        return responseEntity(registrationId, requestEntity);
    }

    private ResponseEntity<JsonNode> responseEntity(String registrationId, RequestEntity<?> requestEntity) {
        switch (registrationId) {
            case "TEST":
                URI uri = UriComponentsBuilder.fromUri(requestEntity.getUrl()).host(registrationId.replace('_', '-') + "-USER-INFO").build(new Object[0]);
                return this.lbRestTemplate.exchange(new RequestEntity<>(requestEntity.getBody(), requestEntity.getHeaders(), requestEntity.getMethod(), uri), JsonNode.class);
            default:
                return this.restTemplate.exchange(requestEntity, JsonNode.class);
        }
    }
}
