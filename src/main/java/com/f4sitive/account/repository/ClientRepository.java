package com.f4sitive.account.repository;

import com.f4sitive.account.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, String> {
    @Transactional(readOnly = true)
    @Query("SELECT entity FROM Client entity WHERE entity.clientId = (:clientId)")
    Optional<Client> queryByClientId(@Param("clientId") String clientId);
}
