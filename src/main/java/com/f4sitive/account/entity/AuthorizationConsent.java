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
@Table(name = "oauth2_authorization_consent")
public class AuthorizationConsent implements Auditable<String, AuthorizationConsent.ID, Instant>, Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    private AuthorizationConsent.ID id = new AuthorizationConsent.ID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "principal_name", insertable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registered_client_id", insertable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
    private RegisteredClient registeredClient;

    @Lob
    @Basic
    @Convert(converter = SetToCommaDelimitedStringConverter.class)
    private Set<String> authorities = new LinkedHashSet<>();

    @Getter
    @Setter
    @ToString(callSuper = false, onlyExplicitlyIncluded = true)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Embeddable
    public static class ID implements Serializable {
        private static final long serialVersionUID = 1L;
        @ToString.Include
        @Column(name = "registered_client_id", length = RegisteredClient.ID_LENGTH)
        private String registeredClientId;

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
