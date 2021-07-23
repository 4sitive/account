package com.f4sitive.account.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Auditable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Optional;

@DynamicInsert
@DynamicUpdate
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "oauth2_client_registration")
public class ClientRegistration implements Auditable<String, String, Instant>, Serializable {
    private static final long serialVersionUID = 1L;
    public static final int ID_LENGTH = 100;
    @Id
    @Column(length = ClientRegistration.ID_LENGTH, nullable = false)
    private String id;

    @Version
    @Column(nullable = false, columnDefinition = "int default 0")
    private int version;
    @CreatedBy
    @Column(length = User.ID_LENGTH)
    private String createdBy;
    @LastModifiedBy
    @Column(length = User.ID_LENGTH)
    private String lastModifiedBy;
    @CreatedDate
    private Instant createdDate;
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Override
    public Optional<String> getCreatedBy() {
        return Optional.ofNullable(createdBy);
    }

    @Override
    public Optional<Instant> getCreatedDate() {
        return Optional.ofNullable(createdDate);
    }

    @Override
    public Optional<String> getLastModifiedBy() {
        return Optional.ofNullable(lastModifiedBy);
    }

    @Override
    public Optional<Instant> getLastModifiedDate() {
        return Optional.ofNullable(lastModifiedDate);
    }

    @Override
    public boolean isNew() {
        return getId() == null;
    }
}
