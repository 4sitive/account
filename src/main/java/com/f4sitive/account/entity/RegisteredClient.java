package com.f4sitive.account.entity;

import com.f4sitive.account.converter.MapToJsonStringConverter;
import com.f4sitive.account.converter.SetToCommaDelimitedStringConverter;
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
import java.util.*;

@DynamicInsert
@DynamicUpdate
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "oauth2_registered_client", uniqueConstraints = @UniqueConstraint(name = "registered_client_ux_client_id", columnNames = {"client_id"}))
public class RegisteredClient implements Auditable<String, String, Instant>, Serializable {
    private static final long serialVersionUID = 1L;
    public static final int ID_LENGTH = 100;
    @Id
    @Column(length = RegisteredClient.ID_LENGTH, nullable = false)
    private String id;
    @Column(name = "client_id", length = 100, nullable = false)
    private String clientId;
    @Column(nullable = false)
    private Instant clientIdIssuedAt;
    @Lob
    @Basic
    private String clientSecret;
    private Instant clientSecretExpiresAt;
    @Column(length = 200, nullable = false)
    private String clientName;
    @Convert(converter = SetToCommaDelimitedStringConverter.class)
    @Lob
    @Basic
    @Column(nullable = false)
    private Set<String> clientAuthenticationMethods = new LinkedHashSet<>();
    @Convert(converter = SetToCommaDelimitedStringConverter.class)
    @Lob
    @Basic
    @Column(nullable = false)
    private Set<String> authorizationGrantTypes = new LinkedHashSet<>();
    @Convert(converter = SetToCommaDelimitedStringConverter.class)
    @Lob
    @Basic
    private Set<String> redirectUris = new LinkedHashSet<>();
    @Convert(converter = SetToCommaDelimitedStringConverter.class)
    @Lob
    @Basic
    private Set<String> scopes = new LinkedHashSet<>();
    @Convert(converter = MapToJsonStringConverter.class)
    @Lob
    @Basic
    @Column(nullable = false, updatable = false)
    private Map<String, Object> clientSettings = new LinkedHashMap<>();
    @Convert(converter = MapToJsonStringConverter.class)
    @Lob
    @Basic
    @Column(nullable = false, updatable = false)
    private Map<String, Object> tokenSettings = new LinkedHashMap<>();

    @Version
    @Column(nullable = false, columnDefinition = "int default 0")
    private long version;
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
