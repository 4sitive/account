package com.f4sitive.account.entity;

import com.f4sitive.account.entity.converter.JsonNodeToStringConverter;
import com.f4sitive.account.entity.converter.SetToCommaDelimitedStringConverter;
import com.f4sitive.account.util.Snowflakes;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
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
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
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

//@Audited
@DynamicInsert
@DynamicUpdate
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@ToString(callSuper = false, onlyExplicitlyIncluded = true)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@NoArgsConstructor
@Entity
@Table(name = "device",
        indexes = {
                @Index(name = "device_ix_user_id", columnList = "user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "device_ux_client_id_serial_number_user_id", columnNames = {"client_id", "serial_number", "user_id"}),
                @UniqueConstraint(name = "device_ux_authorization_code", columnNames = {"authorization_code"}),
                @UniqueConstraint(name = "device_ux_refresh_token", columnNames = {"refresh_token"}),
                @UniqueConstraint(name = "device_ux_state", columnNames = {"state"})
        })
public class Device implements Auditable<String, String, Instant>, Serializable {
    private static final long serialVersionUID = 1L;

    @ToString.Include
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(generator = Constants.SNOWFLAKE_IDENTIFIER_GENERATOR)
    @GenericGenerator(name = Constants.SNOWFLAKE_IDENTIFIER_GENERATOR, strategy = Constants.SNOWFLAKE_IDENTIFIER_GENERATOR_STRATEGY)
    @Column(length = Constants.ID_LENGTH, nullable = false)
    private String id;

    @ToString.Include
    @Column(name = "client_id", length = 100, nullable = false)
    private String clientId;

    @ToString.Include
    @Column(name = "external_serial_number", length = 100)
    private String externalSerialNumber;

    @ToString.Include
    @Column(name = "user_id", length = Constants.ID_LENGTH)
    private String userId;

    @ToString.Include
    @Column(name = "serial_number", length = 100, nullable = false)
    private String serialNumber;

    @ToString.Include
    @Column(length = 100, nullable = false)
    private String authorizationGrantType;

    @ToString.Include
    @Column(length = 50)
    private String state;

    @ToString.Include
    @Convert(converter = SetToCommaDelimitedStringConverter.class)
    @Lob
    @Basic
    private Set<String> accessTokenScopes = new LinkedHashSet<>();

    @Convert(converter = JsonNodeToStringConverter.class)
    @Lob
    @Basic
    private JsonNode attributes;

    @ToString.Include
    @Column(name = "authorization_code", length = 50)
    private String authorizationCode;
    private Instant authorizationCodeIssuedAt;
    private Instant authorizationCodeExpiresAt;
    @Convert(converter = JsonNodeToStringConverter.class)
    @Lob
    @Basic
    private JsonNode authorizationCodeMetadata;

    @ToString.Include
    @Column(name = "access_token")
    @Lob
    @Basic
    private String accessToken;
    private Instant accessTokenIssuedAt;
    private Instant accessTokenExpiresAt;

    @Convert(converter = JsonNodeToStringConverter.class)
    @Lob
    @Basic
    private JsonNode accessTokenMetadata;

    @ToString.Include
    @Column(name = "refresh_token", length = 50)
    private String refreshToken;
    private Instant refreshTokenIssuedAt;
    private Instant refreshTokenExpiresAt;
    @Convert(converter = JsonNodeToStringConverter.class)
    @Lob
    @Basic
    private JsonNode refreshTokenMetadata;

    public String id(Snowflakes snowflakes) {
        return Constants.id(snowflakes);
    }

    public String token() {
        return Constants.uuid(id).toString().replaceAll("[-]", "").toUpperCase();
    }

    public String serialNumber() {
        return this.externalSerialNumber != null && !this.externalSerialNumber.equals(this.serialNumber) && this.serialNumber.equals(this.userId) ? this.externalSerialNumber : this.serialNumber;
    }

    public Map<String, Object> getAttributes() {
        return Optional.ofNullable(Constants.OBJECT_MAPPER.convertValue(attributes, Map.class)).filter(map -> !map.isEmpty()).orElse(Collections.emptyMap());
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = Optional.ofNullable(attributes).filter(map -> !map.isEmpty()).map(map -> Constants.OBJECT_MAPPER.convertValue(map, JsonNode.class)).orElse(null);
    }

    public Map<String, Object> getAuthorizationCodeMetadata() {
        return Optional.ofNullable(Constants.OBJECT_MAPPER.convertValue(authorizationCodeMetadata, Map.class)).filter(map -> !map.isEmpty()).orElse(Collections.emptyMap());
    }

    public void setAuthorizationCodeMetadata(Map<String, Object> authorizationCodeMetadata) {
        this.authorizationCodeMetadata = Optional.ofNullable(authorizationCodeMetadata).filter(map -> !map.isEmpty()).map(map -> Constants.OBJECT_MAPPER.convertValue(map, JsonNode.class)).orElse(null);
    }

    public Map<String, Object> getAccessTokenMetadata() {
        return Optional.ofNullable(Constants.OBJECT_MAPPER.convertValue(accessTokenMetadata, Map.class)).filter(map -> !map.isEmpty()).orElse(Collections.emptyMap());
    }

    public void setAccessTokenMetadata(Map<String, Object> accessTokenMetadata) {
        this.accessTokenMetadata = Optional.ofNullable(accessTokenMetadata).filter(map -> !map.isEmpty()).map(map -> Constants.OBJECT_MAPPER.convertValue(map, JsonNode.class)).orElse(null);
    }

    public Map<String, Object> getRefreshTokenMetadata() {
        return Optional.ofNullable(Constants.OBJECT_MAPPER.convertValue(refreshTokenMetadata, Map.class)).filter(map -> !map.isEmpty()).orElse(Collections.emptyMap());
    }

    public void setRefreshTokenMetadata(Map<String, Object> refreshTokenMetadata) {
        this.refreshTokenMetadata = Optional.ofNullable(refreshTokenMetadata).filter(map -> !map.isEmpty()).map(map -> Constants.OBJECT_MAPPER.convertValue(map, JsonNode.class)).orElse(null);
    }

    @Version
    @Column(nullable = false, columnDefinition = "int default 0")
    private int version;
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
