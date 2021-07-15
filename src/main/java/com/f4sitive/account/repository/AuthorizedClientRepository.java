package com.f4sitive.account.repository;

import com.f4sitive.account.entity.AuthorizedClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository("repository.AuthorizedClientRepository")
public interface AuthorizedClientRepository extends JpaRepository<AuthorizedClient, UUID> {
    @Transactional(readOnly = true)
    @Query("SELECT entity FROM AuthorizedClient entity JOIN FETCH entity.user WHERE entity.clientRegistrationId = (:clientRegistrationId) AND entity.user.id = (:userId)")
    Optional<AuthorizedClient> queryByRegistrationIdAndUserId(@Param("clientRegistrationId") String clientRegistrationId, @Param("userId") String userId);

    @Transactional(readOnly = true)
    @Query("SELECT entity FROM AuthorizedClient entity JOIN FETCH entity.user WHERE entity.clientRegistrationId = (:clientRegistrationId) AND entity.user.username = (:username)")
    Optional<AuthorizedClient> queryByRegistrationIdAndUserUsername(@Param("clientRegistrationId") String clientRegistrationId, @Param("username") String username);
}
