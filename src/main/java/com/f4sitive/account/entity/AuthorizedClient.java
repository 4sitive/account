package com.f4sitive.account.entity;

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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "oauth2_authorized_client")
public class AuthorizedClient implements Auditable<String, UUID, Instant>, Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "generator")
//    @GenericGenerator(name = "generator", strategy = "uuid2")
    @Column(length = 36)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "principal_name", referencedColumnName = "username", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
    private User user;

    @Column
    private String clientRegistrationId;

    @Lob
    @Column
    private String accessTokenValue;

    @Lob
    @Column
    private String refreshTokenValue;

    @Column
    private String accessTokenType;

    @Column
    private Instant accessTokenIssuedAt;

    @Column
    private Instant accessTokenExpiresAt;

    @Column
    private Instant refreshTokenIssuedAt;

    @Convert(converter = StringSetToWhiteSpaceDelimitedStringConverter.class)
    @Column
    private Set<String> accessTokenScopes = new LinkedHashSet<>();

    @Version
    @Column
    private long version;

    @CreatedBy
    @Column
    private String createdBy;

    @LastModifiedBy
    @Column
    private String lastModifiedBy;

    @CreatedDate
    @Column
    private Instant createdDate;

    @LastModifiedDate
    @Column
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
