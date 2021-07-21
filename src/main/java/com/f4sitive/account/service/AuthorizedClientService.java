package com.f4sitive.account.service;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.JdbcOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthorizedClientService extends JdbcOAuth2AuthorizedClientService {
    public AuthorizedClientService(JdbcOperations jdbcOperations, ClientRegistrationRepository clientRegistrationRepository) {
        super(jdbcOperations, clientRegistrationRepository, lobHandler());
    }

    static LobHandler lobHandler() {
        DefaultLobHandler lobHandler = new DefaultLobHandler();
        lobHandler.setCreateTemporaryLob(true);
        lobHandler.setStreamAsLob(true);
        lobHandler.setWrapAsLob(true);
        return new DefaultLobHandler();
    }

    @Transactional(readOnly = true)
    @Override
    public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String clientRegistrationId, String principalName) {
        return super.loadAuthorizedClient(clientRegistrationId, principalName);
    }

    @Transactional
    @Override
    public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal) {
        super.saveAuthorizedClient(authorizedClient, principal);
    }

    @Transactional
    @Override
    public void removeAuthorizedClient(String clientRegistrationId, String principalName) {
        super.removeAuthorizedClient(clientRegistrationId, principalName);
    }
}
