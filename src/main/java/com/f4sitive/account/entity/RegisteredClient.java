package com.f4sitive.account.entity;

import com.f4sitive.account.converter.MapToJsonStringConverter;
import com.f4sitive.account.converter.SetToCommaDelimitedStringConverter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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

@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "oauth2_registered_client")
public class RegisteredClient implements Auditable<String, String, Instant>, Serializable {
    @Id
    private String id;
    private String clientId;
    private Instant clientIdIssuedAt;
    private String clientSecret;
    private Instant clientSecretExpiresAt;
    private String clientName;
    @Convert(converter = SetToCommaDelimitedStringConverter.class)
    private Set<String> clientAuthenticationMethods = new LinkedHashSet<>();
    @Convert(converter = SetToCommaDelimitedStringConverter.class)
    private Set<String> authorizationGrantTypes = new LinkedHashSet<>();
    @Convert(converter = SetToCommaDelimitedStringConverter.class)
    private Set<String> redirectUris = new LinkedHashSet<>();
    @Convert(converter = SetToCommaDelimitedStringConverter.class)
    private Set<String> scopes = new LinkedHashSet<>();
    @Convert(converter = MapToJsonStringConverter.class)
    @Lob
    @Basic
    private Map<String, Object> clientSettings = new LinkedHashMap<>();
    @Convert(converter = MapToJsonStringConverter.class)
    @Lob
    @Basic
    private Map<String, Object> tokenSettings = new LinkedHashMap<>();

    @Version
    private Long version;
    @CreatedBy
    private String createdBy;
    @LastModifiedBy
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
