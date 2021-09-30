package com.f4sitive.account.entity;

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
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Version;
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
@NoArgsConstructor
@ToString(callSuper = false, onlyExplicitlyIncluded = true)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Entity
@Table(name = "authorization_consent",
        indexes = @Index(name = "authorization_consent_ix_user_id", columnList = "user_id"))
public class AuthorizationConsent implements Auditable<String, AuthorizationConsent.ID, Instant>, Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    @ToString.Include
    @EqualsAndHashCode.Include
    private ID id = new ID();

    @Lob
    @Basic
    @Convert(converter = SetToCommaDelimitedStringConverter.class)
    @ToString.Include
    private Set<String> authorities = new LinkedHashSet<>();

    public AuthorizationConsent(ID id) {
        this.id = id;
    }

    @Getter
    @NoArgsConstructor
    @ToString(callSuper = false, onlyExplicitlyIncluded = true)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Embeddable
    public static class ID implements Serializable {
        private static final long serialVersionUID = 1L;
        @ToString.Include
        @Column(name = "user_id", length = Constants.ID_LENGTH)
        private String userId;

        @ToString.Include
        @Column(name = "client_id", length = 100)
        private String clientId;

        public ID(String userId, String clientId) {
            this.userId = userId;
            this.clientId = clientId;
        }
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
