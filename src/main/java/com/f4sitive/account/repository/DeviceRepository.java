package com.f4sitive.account.repository;

import com.f4sitive.account.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, String> {
    @Transactional(readOnly = true)
    @Query("SELECT entity FROM Device entity WHERE entity.clientId = (:clientId) AND entity.serialNumber = (:serialNumber) AND entity.userId = (:userId)")
    Optional<Device> queryByClientIdAndSerialNumberAndUserId(@Param("clientId") String clientId, @Param("serialNumber") String serialNumber, @Param("userId") String userId);

    @Transactional(readOnly = true)
    @Query("SELECT entity FROM Device entity WHERE entity.authorizationCode = (:authorizationCode)")
    Optional<Device> queryByAuthorizationCode(@Param("authorizationCode") String authorizationCode);

    @Transactional(readOnly = true)
    @Query("SELECT entity FROM Device entity WHERE entity.refreshToken = (:refreshToken)")
    Optional<Device> queryByRefreshToken(@Param("refreshToken") String refreshToken);

    @Transactional(readOnly = true)
    @Query("SELECT entity FROM Device entity WHERE entity.state = (:state)")
    Optional<Device> queryByState(@Param("state") String state);

    @Transactional(readOnly = true)
    @Query("SELECT entity FROM Device entity WHERE entity.userId = (:userId)")
    List<Device> queryAllByUserId(@Param("userId") String userId);
}
