package com.f4sitive.account.service;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthorizationConsentService extends JdbcOAuth2AuthorizationConsentService {
    public AuthorizationConsentService(JdbcOperations jdbcOperations, RegisteredClientRepository registeredClientRepository) {
        super(jdbcOperations, registeredClientRepository);
    }

    @Transactional
    @Override
    public void save(OAuth2AuthorizationConsent authorizationConsent) {
        super.save(authorizationConsent);
    }

    @Transactional
    @Override
    public void remove(OAuth2AuthorizationConsent authorizationConsent) {
        super.remove(authorizationConsent);
    }

    @Transactional(readOnly = true)
    @Override
    public OAuth2AuthorizationConsent findById(String registeredClientId, String principalName) {
        return super.findById(registeredClientId, principalName);
    }
}
