package com.f4sitive.account.service;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.security.oauth2.core.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthorizationService extends JdbcOAuth2AuthorizationService {
    public AuthorizationService(JdbcOperations jdbcOperations, RegisteredClientRepository registeredClientRepository) {
        super(jdbcOperations, registeredClientRepository, lobHandler());
    }

    static LobHandler lobHandler() {
        DefaultLobHandler lobHandler = new DefaultLobHandler();
        lobHandler.setCreateTemporaryLob(true);
        lobHandler.setStreamAsLob(true);
        lobHandler.setWrapAsLob(true);
        return new DefaultLobHandler();
    }

    @Override
    @Transactional
    public void save(OAuth2Authorization authorization) {
        super.save(authorization);
    }

    @Override
    @Transactional
    public void remove(OAuth2Authorization authorization) {
        super.remove(authorization);
    }

    @Override
    @Transactional(readOnly = true)
    public OAuth2Authorization findById(String id) {
        return super.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        return super.findByToken(token, tokenType);
    }
}
