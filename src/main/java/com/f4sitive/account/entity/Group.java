package com.f4sitive.account.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Auditable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
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
@Entity
@NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(name = "group_ux_name", columnNames = {"name"}))
public class Group implements Auditable<String, String, Instant>, Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "generator")
    @GenericGenerator(name = "generator", strategy = "uuid2")
    @Column(length = 36)
    private String id;

    @Column(length = 200, nullable = false)
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "group_authority", joinColumns = @JoinColumn(foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT)))
    @Column(name = "name", length = 200)
    private Set<String> authority = new LinkedHashSet<>();

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
