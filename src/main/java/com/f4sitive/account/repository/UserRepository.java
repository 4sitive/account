package com.f4sitive.account.repository;

import com.f4sitive.account.entity.AuthorizedClient;
import com.f4sitive.account.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
//    @Transactional(readOnly = true)
//    @Query("SELECT entity FROM User entity JOIN FETCH entity.authorizedClients WHERE entity.registrationId = (:registrationId) AND entity.user.id = (:userId)")
//    Optional<AuthorizedClient> queryByRegistrationIdAndUserId(@Param("registrationId") String registrationId, @Param("userId") String userId);
}
