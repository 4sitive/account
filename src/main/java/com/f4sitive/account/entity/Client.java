package com.f4sitive.account.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.f4sitive.account.entity.converter.JsonNodeToStringConverter;
import com.f4sitive.account.entity.converter.SetToCommaDelimitedStringConverter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Auditable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Getter
@Setter
@ToString(callSuper = false, onlyExplicitlyIncluded = true)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@NoArgsConstructor
@DynamicInsert
@DynamicUpdate
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "client",
        uniqueConstraints = @UniqueConstraint(name = "client_ux_client_id", columnNames = {"client_id"}))
public class Client implements Auditable<String, String, Instant>, Serializable {
    private static final long serialVersionUID = 1L;

    @ToString.Include
    @Id
    @Column(length = 100, nullable = false)
    private String id;

    @ToString.Include
    @EqualsAndHashCode.Include
    @Column(name = "client_id", length = 100, nullable = false)
    private String clientId;

    @ToString.Include
    @Column(nullable = false)
    private Instant clientIdIssuedAt;

    @Lob
    @Basic
    private String clientSecret;
    private Instant clientSecretExpiresAt;

    @ToString.Include
    @Column(length = 200, nullable = false)
    private String clientName;

    @ToString.Include
    @Convert(converter = SetToCommaDelimitedStringConverter.class)
    @Lob
    @Basic
    @Column(nullable = false)
    private Set<String> clientAuthenticationMethods = new LinkedHashSet<>();

    @ToString.Include
    @Convert(converter = SetToCommaDelimitedStringConverter.class)
    @Lob
    @Basic
    @Column(nullable = false)
    private Set<String> authorizationGrantTypes = new LinkedHashSet<>();

    @ToString.Include
    @Convert(converter = SetToCommaDelimitedStringConverter.class)
    @Lob
    @Basic
    private Set<String> redirectUris = new LinkedHashSet<>();

    @ToString.Include
    @Convert(converter = SetToCommaDelimitedStringConverter.class)
    @Lob
    @Basic
    private Set<String> scopes = new LinkedHashSet<>();

    @Convert(converter = JsonNodeToStringConverter.class)
    @Lob
    @Basic
    @Column
    private JsonNode clientSettings;

    @Convert(converter = JsonNodeToStringConverter.class)
    @Lob
    @Basic
    @Column
    private JsonNode tokenSettings;

    public Client(String id) {
        this.id = id;
        this.clientId = id;
    }

    public Map<String, Object> getClientSettings() {
        return Optional.ofNullable(Constants.OBJECT_MAPPER.convertValue(clientSettings, Map.class)).filter(map -> !map.isEmpty()).orElse(Collections.emptyMap());
    }

    public void setClientSettings(Map<String, Object> clientSettings) {
        this.clientSettings = Optional.ofNullable(clientSettings).filter(map -> !map.isEmpty()).map(map -> Constants.OBJECT_MAPPER.convertValue(map, JsonNode.class)).orElse(null);
    }

    public Map<String, Object> getTokenSettings() {
        return Optional.ofNullable(Constants.OBJECT_MAPPER.convertValue(tokenSettings, Map.class)).filter(map -> !map.isEmpty()).orElse(Collections.emptyMap());
    }

    public void setTokenSettings(Map<String, Object> tokenSettings) {
        this.tokenSettings = Optional.ofNullable(tokenSettings).filter(map -> !map.isEmpty()).map(map -> Constants.OBJECT_MAPPER.convertValue(map, JsonNode.class)).orElse(null);
    }

    @Version
    @Column(nullable = false, columnDefinition = "int default 0")
    private long version;
    @CreatedBy
    @Column(length = Constants.ID_LENGTH)
    private String createdBy;
    @LastModifiedBy
    @Column(length = Constants.ID_LENGTH)
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
