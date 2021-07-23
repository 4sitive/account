package com.f4sitive.account.entity;

import com.f4sitive.account.converter.SetToCommaDelimitedStringConverter;
import lombok.*;
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
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

@DynamicInsert
@DynamicUpdate
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "oauth2_authorized_client", uniqueConstraints = @UniqueConstraint(name = "authorized_client_ux_client_registration_id_principal_name", columnNames = {"client_registration_id", "principal_name"}))
public class AuthorizedClient implements Auditable<String, AuthorizedClient.ID, Instant>, Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    private AuthorizedClient.ID id = new AuthorizedClient.ID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(insertable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
    private ClientRegistration clientRegistration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "principal_name", insertable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
    private User user;

    @Column(length = 50)
    private String accessTokenType;
    private Instant accessTokenExpiresAt;

    @Lob
    @Basic
    private String accessTokenValue;
    private Instant accessTokenIssuedAt;
    @Lob
    @Basic
    private String refreshTokenValue;
    private Instant refreshTokenIssuedAt;

    @Convert(converter = SetToCommaDelimitedStringConverter.class)
    @Lob
    @Basic
    private Set<String> accessTokenScopes = new LinkedHashSet<>();

    @Getter
    @Setter
    @ToString(callSuper = false, onlyExplicitlyIncluded = true)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Embeddable
    public static class ID implements Serializable {
        private static final long serialVersionUID = 1L;
        @ToString.Include
        @Column(name = "client_registration_id", length = ClientRegistration.ID_LENGTH)
        private String clientRegistrationId;

        @ToString.Include
        @Column(name = "principal_name", length = User.ID_LENGTH)
        private String userId;
    }

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
