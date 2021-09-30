package com.f4sitive.account.repository;

import com.f4sitive.account.entity.AuthorizedClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AuthorizedClientRepository extends JpaRepository<AuthorizedClient, AuthorizedClient.ID> {
    @Transactional(readOnly = true)
    @Query("SELECT entity FROM AuthorizedClient entity WHERE entity.id.userId = (:userId)")
    List<AuthorizedClient> queryAllByUserId(@Param("userId") String userId);
}
