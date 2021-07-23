package com.f4sitive.account.service;

import com.f4sitive.account.repository.RegisteredClientRepository;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.function.Function;

@Service
public class RegisteredClientService extends JdbcRegisteredClientRepository {
    private final RegisteredClientRepository registeredClientRepository;

    public RegisteredClientService(JdbcOperations jdbcOperations, RegisteredClientRepository registeredClientRepository) {
        super(jdbcOperations);
        this.registeredClientRepository = registeredClientRepository;
    }

    @Transactional
    public Optional<com.f4sitive.account.entity.RegisteredClient> save(String id, Function<com.f4sitive.account.entity.RegisteredClient, com.f4sitive.account.entity.RegisteredClient> function) {
        return registeredClientRepository.findById(id)
                .map(function);
    }

    @Override
    @Transactional
    public void save(RegisteredClient registeredClient) {
        super.save(registeredClient);
    }

    @Override
    @Transactional(readOnly = true)
    public RegisteredClient findById(String id) {
        return super.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public RegisteredClient findByClientId(String clientId) {
        return super.findByClientId(clientId);
    }
}
