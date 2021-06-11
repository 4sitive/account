package com.f4sitive.account.entity;

import com.f4sitive.account.converter.StringSetToCommaDelimitedStringConverter;
import com.f4sitive.account.converter.StringSetToWhiteSpaceDelimitedStringConverter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Auditable;

import javax.persistence.*;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "CLIENT")
public class Client implements Auditable<String, String, ZonedDateTime>, Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "generator")
    @GenericGenerator(name = "generator", strategy = "uuid2")
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "CLIENT_ID")
    private String clientId;

    @Column(name = "CLIENT_SECRET")
    private String clientSecret;

    @Column(name = "NAME")
    private String name;

    @Convert(converter = StringSetToCommaDelimitedStringConverter.class)
    @Column(name = "CLIENT_AUTHENTICATION_METHODS")
    private Set<String> clientAuthenticationMethods = new LinkedHashSet<>();

    @Convert(converter = StringSetToWhiteSpaceDelimitedStringConverter.class)
    @Column(name = "SCOPES")
    private Set<String> scopes = new LinkedHashSet<>();

    @Convert(converter = StringSetToCommaDelimitedStringConverter.class)
    @Column(name = "AUTHORIZATION_GRANT_TYPES")
    private Set<String> authorizationGrantTypes = new LinkedHashSet<>();

    @Convert(converter = StringSetToCommaDelimitedStringConverter.class)
    @Column(name = "REDIRECT_URIS")
    private Set<String> redirectUris = new LinkedHashSet<>();

    @Version
    @Column(name = "VERSION")
    private long version;

    @CreatedBy
    @Column(name = "CREATED_BY")
    private String createdBy;

    @LastModifiedBy
    @Column(name = "LAST_MODIFIED_BY")
    private String lastModifiedBy;

    @CreatedDate
    @Column(name = "CREATED_DATE")
    private ZonedDateTime createdDate;

    @LastModifiedDate
    @Column(name = "LAST_MODIFIED_DATE")
    private ZonedDateTime lastModifiedDate;

    @Override
    public Optional<String> getCreatedBy() {
        return Optional.ofNullable(createdBy);
    }

    @Override
    public Optional<ZonedDateTime> getCreatedDate() {
        return Optional.ofNullable(createdDate);
    }

    @Override
    public Optional<String> getLastModifiedBy() {
        return Optional.ofNullable(lastModifiedBy);
    }

    @Override
    public Optional<ZonedDateTime> getLastModifiedDate() {
        return Optional.ofNullable(lastModifiedDate);
    }

    @Override
    public boolean isNew() {
        return getId() == null;
    }
}
