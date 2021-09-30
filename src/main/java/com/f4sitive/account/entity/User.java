package com.f4sitive.account.entity;

import com.f4sitive.account.entity.listener.UserEntityListener;
import com.f4sitive.account.util.Snowflakes;
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
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import java.io.Serializable;
import java.time.Instant;
import java.util.Optional;

@DynamicInsert
@DynamicUpdate
@EntityListeners(value = {AuditingEntityListener.class, UserEntityListener.class})
@Getter
@Setter
@ToString(callSuper = false, onlyExplicitlyIncluded = true)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@NoArgsConstructor
@Entity
@Document
@Table(name = "user",
        indexes = @Index(name = "user_ix_parent_id", columnList = "parent_id"),
        uniqueConstraints = @UniqueConstraint(name = "user_ux_username_registration_id", columnNames = {"username", "registration_id"}))
public class User implements Auditable<String, String, Instant>, Serializable {
    private static final long serialVersionUID = 1L;

    @ToString.Include
    @Id
    @org.springframework.data.annotation.Id
    @GeneratedValue(generator = Constants.SNOWFLAKE_IDENTIFIER_GENERATOR)
    @GenericGenerator(name = Constants.SNOWFLAKE_IDENTIFIER_GENERATOR, strategy = Constants.SNOWFLAKE_IDENTIFIER_GENERATOR_STRATEGY)
    @Column(length = Constants.ID_LENGTH)
    private String id;

    @ToString.Include
    @EqualsAndHashCode.Include
    @Column(length = 100, nullable = false)
    private String username;

    @ToString.Include
    @EqualsAndHashCode.Include
    @Column(name = "registration_id", length = 100)
    private String registrationId;

    @ToString.Include
    @Column(length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
    private User parent;

    public User(String id) {
        this.id = id;
    }

    public User(String username, String registrationId) {
        this.username = username;
        this.registrationId = registrationId;
    }

    public String id(Snowflakes snowflakes) {
        return Constants.id(snowflakes);
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
