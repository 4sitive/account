package com.f4sitive.account.entity;

import com.f4sitive.account.converter.StringSetToCommaDelimitedStringConverter;
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(name = "ux_user_username", columnNames = {"username"}))
public class User implements Auditable<String, String, Instant>, Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "generator")
    @GenericGenerator(name = "generator", strategy = "uuid2")
    @Column(length = 36)
    private String id;

    @Convert(converter = StringSetToCommaDelimitedStringConverter.class)
    @Column
    private Set<String> authorities = new LinkedHashSet<>();

    @Column(length = 200)
    private String name;

    @Column(length = 200)
    private String displayName;

    @Column
    @Lob
    private String password;

    @Column
    @Lob
    private String introduce;

    @Column(length = 200)
    private String username;

    @Column(length = 200)
    private String email;

    @Column(length = 200)
    private String image;

//    @ElementCollection(fetch = FetchType.EAGER)
//    @JoinTable(name = "USER_ATTRIBUTE", joinColumns = @JoinColumn(name = "USER_ID", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT)))
//    @MapKeyColumn(name = "KEY", length = 45)
//    @Lob
//    @Column(name = "VALUE", nullable = false)
//    private Map<String, String> attribute = new HashMap<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AuthorizedClient> authorizedClients = new HashSet<>();

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

    public User(String id) {
        this.id = id;
    }

    public static User of(String username) {
        User user = new User();
        user.setUsername(username);
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
