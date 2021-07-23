package com.f4sitive.account.repository;

import com.f4sitive.account.entity.RegisteredClient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegisteredClientRepository extends JpaRepository<RegisteredClient, String> {
}
