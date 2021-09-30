package com.f4sitive.account.service;

import com.f4sitive.account.entity.AuthorizationConsent;
import com.f4sitive.account.repository.AuthorizationConsentRepository;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.stream.Collectors;

@Service
public class AuthorizationConsentService extends JdbcOAuth2AuthorizationConsentService {
    private final AuthorizationConsentRepository authorizationConsentRepository;

    public AuthorizationConsentService(JdbcOperations jdbcOperations,
                                       RegisteredClientRepository registeredClientRepository,
                                       AuthorizationConsentRepository authorizationConsentRepository) {
        super(jdbcOperations, registeredClientRepository);
        this.authorizationConsentRepository = authorizationConsentRepository;
    }

    @Transactional
    @Override
    public void save(OAuth2AuthorizationConsent oauth2AuthorizationConsent) {
        String clientId = oauth2AuthorizationConsent.getRegisteredClientId();
        String userId = oauth2AuthorizationConsent.getPrincipalName();
        AuthorizationConsent.ID id = new AuthorizationConsent.ID(userId, clientId);
        AuthorizationConsent authorizationConsent = authorizationConsentRepository.findById(id)
                .orElseGet(() -> new AuthorizationConsent(id));
        authorizationConsent.setAuthorities(oauth2AuthorizationConsent.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toCollection(LinkedHashSet::new)));
        authorizationConsentRepository.save(authorizationConsent);
    }

    @Transactional
    @Override
    public void remove(OAuth2AuthorizationConsent oauth2AuthorizationConsent) {
        String clientId = oauth2AuthorizationConsent.getRegisteredClientId();
        String userId = oauth2AuthorizationConsent.getPrincipalName();
        AuthorizationConsent.ID id = new AuthorizationConsent.ID(userId, clientId);
        authorizationConsentRepository.findById(id)
                .ifPresent(authorizationConsentRepository::delete);
    }

    @Transactional(readOnly = true)
    @Override
    public OAuth2AuthorizationConsent findById(String clientId, String userId) {
        return authorizationConsentRepository.findById(new AuthorizationConsent.ID(userId, clientId))
                .map(authorizationConsent -> OAuth2AuthorizationConsent.withId(authorizationConsent.getId().getClientId(), authorizationConsent.getId().getUserId())
                        .authorities(grantedAuthorities -> grantedAuthorities.addAll(AuthorityUtils.createAuthorityList(authorizationConsent.getAuthorities().stream().toArray(String[]::new))))
                        .build())
                .orElse(null);
    }
}
