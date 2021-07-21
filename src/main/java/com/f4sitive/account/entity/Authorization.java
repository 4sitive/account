package com.f4sitive.account.entity;

import com.f4sitive.account.converter.MapToJsonStringConverter;
import com.f4sitive.account.converter.SetToCommaDelimitedStringConverter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
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
@Table(name = "oauth2_authorization")
public class Authorization implements Auditable<String, String, Instant>, Serializable {
    @Id
    @Column(length = 100, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
    private RegisteredClient registeredClient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "principal_name", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
    private User user;

    @Column(length = 100, nullable = false)
    private String authorizationGrantType;

    @Convert(converter = MapToJsonStringConverter.class)
    @Lob
    @Basic
    private Map<String, Object> attributes = new LinkedHashMap<>();

    @Column(length = 500)
    private String state;

    @Column(length = 100)
    private String accessTokenType;

    @Convert(converter = SetToCommaDelimitedStringConverter.class)
    @Lob
    @Basic
    private Set<String> accessTokenScopes = new LinkedHashSet<>();

    @Lob
    private byte[] accessTokenValue;

    public String getAccessToken() {
        return Optional.ofNullable(accessTokenValue).map(String::new).orElse(null);
    }

    private Instant accessTokenIssuedAt;
    private Instant accessTokenExpiresAt;
    @Convert(converter = MapToJsonStringConverter.class)
    @Lob
    private Map<String, Object> accessTokenMetadata = new LinkedHashMap<>();

    @Lob
    private byte[] authorizationCodeValue;
    public String getAuthorizationCode() {
        return Optional.ofNullable(authorizationCodeValue).map(String::new).orElse(null);
    }
    private Instant authorizationCodeIssuedAt;
    private Instant authorizationCodeExpiresAt;
    @Convert(converter = MapToJsonStringConverter.class)
    @Lob
    private Map<String, Object> authorizationCodeMetadata = new LinkedHashMap<>();

    @Lob
    private byte[] oidcIdTokenValue;
    public String getOidcIdToken() {
        return Optional.ofNullable(authorizationCodeValue).map(String::new).orElse(null);
    }
    private Instant oidcIdTokenIssuedAt;
    private Instant oidcIdTokenExpiresAt;
    @Convert(converter = MapToJsonStringConverter.class)
    @Lob
    @Basic
    private Map<String, Object> oidcIdTokenMetadata = new LinkedHashMap<>();

    @Lob
    private byte[] refreshTokenValue;
    public String getRefreshToken() {
        return Optional.ofNullable(authorizationCodeValue).map(String::new).orElse(null);
    }
    private Instant refreshTokenIssuedAt;
    private Instant refreshTokenExpiresAt;
    @Convert(converter = MapToJsonStringConverter.class)
    @Lob
    @Basic
    private Map<String, Object> refreshTokenMetadata = new LinkedHashMap<>();

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
