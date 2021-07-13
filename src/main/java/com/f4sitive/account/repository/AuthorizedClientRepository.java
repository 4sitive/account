package com.f4sitive.account.repository;

import com.f4sitive.account.entity.AuthorizedClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository("repository.AuthorizedClientRepository")
public interface AuthorizedClientRepository extends JpaRepository<AuthorizedClient, String> {
    @Transactional(readOnly = true)
    @Query("SELECT entity FROM AuthorizedClient entity JOIN FETCH entity.user WHERE entity.registrationId = (:registrationId) AND entity.user.id = (:userId)")
    Optional<AuthorizedClient> queryByRegistrationIdAndUserId(@Param("registrationId") String registrationId, @Param("userId") String userId);

    @Transactional(readOnly = true)
    @Query("SELECT entity FROM AuthorizedClient entity JOIN FETCH entity.user WHERE entity.registrationId = (:registrationId) AND entity.user.username = (:username)")
    Optional<AuthorizedClient> queryByRegistrationIdAndUserUsername(@Param("registrationId") String registrationId, @Param("username") String username);
}
