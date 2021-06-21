package com.f4sitive.account.service;

import com.f4sitive.account.entity.Client;
import com.f4sitive.account.repository.ClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;

//@Service
public class ClientService implements RegisteredClientRepository {
    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Override
    public void save(RegisteredClient registeredClient) {

    }

    @Override
    public RegisteredClient findById(String id) {
        return clientRepository.findById(id)
                .map(this::converter)
                .orElse(null);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        return clientRepository.queryByClientId(clientId)
                .map(this::converter)
                .orElse(null);
    }

    private RegisteredClient converter(Client client) {
        return RegisteredClient.withId(client.getId())
                .clientId(client.getClientId())
                .clientSecret(client.getClientSecret())
                .clientAuthenticationMethods(consumer -> client.getClientAuthenticationMethods()
                        .stream()
                        .map(ClientAuthenticationMethod::new)
                        .forEach(consumer::add))
                .authorizationGrantTypes(consumer -> client.getAuthorizationGrantTypes()
                        .stream()
                        .map(AuthorizationGrantType::new)
                        .forEach(consumer::add))
                .redirectUris(consumer -> client.getRedirectUris()
                        .forEach(consumer::add))
                .scopes(consumer -> client.getScopes()
                        .forEach(consumer::add))
                .tokenSettings(consumer -> {
                })
                .clientSettings(consumer -> {
                })
                .build();
    }
}
