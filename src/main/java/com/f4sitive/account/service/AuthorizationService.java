package com.f4sitive.account.service;

import org.springframework.cache.Cache;
import org.springframework.security.oauth2.core.OAuth2TokenType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.util.StringUtils;

import java.util.Optional;

public class AuthorizationService implements OAuth2AuthorizationService {
    private final Cache authorizationCode;

    public AuthorizationService(Cache authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    @Override
    public void save(OAuth2Authorization authorization) {
        authorization.getToken(OAuth2AuthorizationCode.class);
    }

    @Override
    public void remove(OAuth2Authorization authorization) {

    }

    @Override
    public OAuth2Authorization findById(String id) {
        return null;
    }

    @Override
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        return Optional.ofNullable(tokenType)
                .map(OAuth2TokenType::getValue)
                .filter(value -> OAuth2ParameterNames.CODE.equals(value) && StringUtils.hasText(token))
                .flatMap(value -> Optional.ofNullable(authorizationCode.get(token, OAuth2Authorization.class))
                        .filter(authorization -> authorization.getToken(OAuth2AuthorizationCode.class).getToken().getTokenValue().equals(token)))
                .orElse(null);
    }
}
