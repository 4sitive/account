package com.f4sitive.account.repository;

import com.f4sitive.account.entity.AuthorizationConsent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorizationConsentRepository extends JpaRepository<AuthorizationConsent, AuthorizationConsent.ID> {
}
