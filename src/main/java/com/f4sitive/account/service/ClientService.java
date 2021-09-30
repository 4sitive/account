package com.f4sitive.account.service;

import com.f4sitive.account.entity.Client;
import com.f4sitive.account.repository.ClientRepository;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.config.ClientSettings;
import org.springframework.security.oauth2.server.authorization.config.TokenSettings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ClientService extends JdbcRegisteredClientRepository {
    private final ClientRepository clientRepository;
    @SuppressWarnings("deprecation")
    private final PasswordEncoder passwordEncoder = new DelegatingPasswordEncoder("noop", Collections.singletonMap("noop", org.springframework.security.crypto.password.NoOpPasswordEncoder.getInstance()));

    public ClientService(JdbcOperations jdbcOperations,
                         ClientRepository clientRepository) {
        super(jdbcOperations);
        this.clientRepository = clientRepository;
    }

    @Override
    @Transactional
    public void save(RegisteredClient registeredClient) {
        Client client = clientRepository.findById(registeredClient.getId())
                .orElseGet(() -> new Client(registeredClient.getId()));
        client.setClientId(registeredClient.getClientId());
        client.setClientIdIssuedAt(registeredClient.getClientIdIssuedAt());
        client.setClientSecret(Optional.ofNullable(registeredClient.getClientSecret()).map(passwordEncoder::encode).orElse(null));
        client.setClientSecretExpiresAt(registeredClient.getClientSecretExpiresAt());
        client.setClientName(registeredClient.getClientName());
        client.setClientAuthenticationMethods(registeredClient.getClientAuthenticationMethods().stream().map(ClientAuthenticationMethod::getValue).collect(Collectors.toCollection(LinkedHashSet::new)));
        client.setAuthorizationGrantTypes(registeredClient.getAuthorizationGrantTypes().stream().map(AuthorizationGrantType::getValue).collect(Collectors.toCollection(LinkedHashSet::new)));
        client.setRedirectUris(registeredClient.getRedirectUris());
        client.setScopes(registeredClient.getScopes());
        client.setClientSettings(registeredClient.getClientSettings().getSettings());
        client.setTokenSettings(registeredClient.getTokenSettings().getSettings());
        clientRepository.save(client);
    }

    @Override
    @Transactional(readOnly = true)
    public RegisteredClient findById(String id) {
        return clientRepository.findById(id)
                .map(this::registeredClient)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public RegisteredClient findByClientId(String clientId) {
        return clientRepository.queryByClientId(clientId)
                .map(this::registeredClient)
                .orElse(null);
    }

    RegisteredClient registeredClient(Client client) {
        return RegisteredClient.withId(client.getId())
                .clientId(client.getClientId())
                .clientIdIssuedAt(client.getClientIdIssuedAt())
                .clientSecret(client.getClientSecret())
                .clientSecretExpiresAt(client.getClientSecretExpiresAt())
                .clientName(client.getClientName())
                .clientAuthenticationMethods((authenticationMethods) -> authenticationMethods.addAll(client.getClientAuthenticationMethods().stream().map(ClientAuthenticationMethod::new).collect(Collectors.toCollection(LinkedHashSet::new))))
                .authorizationGrantTypes((grantTypes) -> grantTypes.addAll(client.getAuthorizationGrantTypes().stream().map(AuthorizationGrantType::new).collect(Collectors.toCollection(LinkedHashSet::new))))
                .redirectUris((uris) -> uris.addAll(client.getRedirectUris()))
                .scopes((scopes) -> scopes.addAll(client.getScopes()))
                .clientSettings(ClientSettings.builder().settings(s -> s.putAll(client.getClientSettings())).build())
                .tokenSettings(TokenSettings.builder().settings(s -> s.putAll(client.getTokenSettings())).build())
                .build();
    }
}
