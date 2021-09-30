package com.f4sitive.account.repository;

import com.f4sitive.account.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    @Transactional(readOnly = true)
    @Query("SELECT entity FROM User entity WHERE entity.username = (:username) AND entity.registrationId = (:registrationId)")
    Optional<User> queryByUsernameAndRegistrationId(@Param("username") String username, @Param("registrationId") String registrationId);

    @Transactional(readOnly = true)
    @Query("SELECT entity FROM User entity WHERE entity.username = (:username) AND entity.registrationId IS NULL")
    Optional<User> queryByUsernameAndRegistrationIdIsNull(@Param("username") String username);

    @Transactional(readOnly = true)
    @Query("SELECT entity FROM User entity WHERE entity.id = (:parentId) OR entity.parent.id = (:parentId)")
    List<User> queryAllByParentId(@Param("parentId") String parentId);

}
