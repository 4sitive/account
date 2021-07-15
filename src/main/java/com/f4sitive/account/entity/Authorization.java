package com.f4sitive.account.entity;

import com.f4sitive.account.converter.MapToJsonStringConverter;
import com.f4sitive.account.converter.StringSetToCommaDelimitedStringConverter;
import com.f4sitive.account.converter.StringSetToWhiteSpaceDelimitedStringConverter;
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
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "oauth2_authorization")
public class Authorization implements Auditable<String, String, Instant>, Serializable {
    @Id
    private String id;

    private String registeredClientId;

//    private User user;

    private String authorizationGrantType;

    @Convert(converter = MapToJsonStringConverter.class)
    @Lob
    private Map<String, Object> attributes;

    private String state;

    private String accessTokenType;

    @Convert(converter = StringSetToCommaDelimitedStringConverter.class)
    @Column
    private Set<String> accessTokenScopes = new LinkedHashSet<>();

//    @Lob
    @Basic
    private byte[] authorizationCodeValue;
    private Instant authorizationCodeIssuedAt;
    private Instant authorizationCodeExpiresAt;
    @Convert(converter = MapToJsonStringConverter.class)
    @Lob
    private Map<String, Object> authorizationCodeMetadata;

    @Basic
    private byte[] oidcIdTokenValue;
    private Instant oidcIdTokenIssuedAt;
    private Instant oidcIdTokenExpiresAt;
    @Convert(converter = MapToJsonStringConverter.class)
    @Lob
    private Map<String, Object> oidcIdTokenMetadata;

    @Basic
    private byte[] refreshTokenValue;
    private Instant refreshTokenIssuedAt;
    private Instant refreshTokenExpiresAt;
    @Convert(converter = MapToJsonStringConverter.class)
    @Lob
    private Map<String, Object> refreshTokenMetadata;

    @Version
    private long version;

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
