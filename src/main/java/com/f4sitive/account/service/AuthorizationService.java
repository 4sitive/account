package com.f4sitive.account.service;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;

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
}
