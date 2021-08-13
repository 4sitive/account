package com.f4sitive.account.entity;

import com.f4sitive.account.entity.listener.UserEntityListener;
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
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import java.io.Serializable;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

@EntityListeners(value = {AuditingEntityListener.class, UserEntityListener.class})
@Getter
@Setter
@Entity
@NoArgsConstructor
@Document
@Table(name = "user", uniqueConstraints = @UniqueConstraint(name = "user_ux_username", columnNames = {"username"}))
public class User implements Auditable<String, String, Instant>, Serializable {
    private static final long serialVersionUID = 1L;
    public static final int ID_LENGTH = 36;
    @Id
    @org.springframework.data.annotation.Id
    @GeneratedValue(generator = "user_identifier_generator")
    @GenericGenerator(name = "user_identifier_generator", strategy = "com.f4sitive.account.entity.generator.UserIdentifierGenerator")
    @Column(length = User.ID_LENGTH)
    private String id;

    @Indexed(unique = true)
    @Column(length = 200, nullable = false)
    private String username;
    @Lob
    @Basic
    private String password;
    private boolean accountExpired;
    private boolean accountLocked;
    private boolean credentialsExpired;
    private boolean disabled;
    @Column(length = 200)
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "user_authority", joinColumns = @JoinColumn(foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT)))
    @Column(name = "name", length = 200)
    private Set<String> authority = new LinkedHashSet<>();

    private String email;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "group_member",
            joinColumns = @JoinColumn(foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT)),
            inverseJoinColumns = @JoinColumn(foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT)),
            foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT),
            inverseForeignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT)
    )
    private Set<Group> group = new LinkedHashSet<>();

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

    public static User of(UserDetails userDetails) {
        User user = new User();
        user.setUsername(userDetails.getUsername());
        user.setPassword(userDetails.getPassword());
        user.setDisabled(!userDetails.isEnabled());
        user.setCredentialsExpired(!userDetails.isCredentialsNonExpired());
        user.setAccountExpired(!userDetails.isAccountNonExpired());
        user.setAccountLocked(!userDetails.isAccountNonLocked());
        return user;
    }

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
