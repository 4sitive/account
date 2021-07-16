package com.f4sitive.account.service;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisteredClientService extends JdbcRegisteredClientRepository {
    public RegisteredClientService(JdbcOperations jdbcOperations) {
        super(jdbcOperations);
    }

    @Override
    @Transactional
    public void save(RegisteredClient registeredClient) {
        try {
            super.save(registeredClient);
        }catch (IllegalArgumentException e){
            //ignore
        }
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
