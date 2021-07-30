package com.f4sitive.account.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Auditable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

@EntityListeners(value = {AuditingEntityListener.class, User.UserEntityListener.class})
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
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "generator")
    @GenericGenerator(name = "generator", strategy = "com.f4sitive.account.entity.User$UserIdentifierGenerator")
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

    public static class UserIdentifierGenerator extends UUIDGenerator {
        private String entityName;
        @Override
        public void configure(Type type, Properties params, ServiceRegistry serviceRegistry) throws MappingException {
            entityName = params.getProperty( ENTITY_NAME );
            if ( entityName == null ) {
                throw new MappingException("no entity name");
            }
            super.configure(type, params, serviceRegistry);
        }
        @Override
        public Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
            Serializable id = session.getEntityPersister(this.entityName, object).getIdentifier(object, session);
            return id == null ? super.generate(session, object) : id;
        }
    }

    public static class UserEntityListener {
        private final MongoOperations mongoOperations;

        public UserEntityListener(MongoOperations mongoOperations) {
            this.mongoOperations = mongoOperations;
        }

        @PrePersist
        public void prePersist(User target){
            Optional.ofNullable(mongoOperations.findOne(Query.query(Criteria.where("username").is(target.getUsername())), User.class))
                    .map(User::getId)
                    .ifPresent(target::setId);
        }

        @PostPersist
        public void postPersist(User target) {
            Assert.notNull(target, "Entity must not be null!");
//            ,
//            FindAndModifyOptions.options().upsert(true),
            mongoOperations.findAndModify(
                    Query.query(Criteria.where("_id").is(target.getId())),
                    Update.update("username", target.getUsername()),
                    FindAndModifyOptions.options().upsert(true),
                    User.class
            );
        }
    }
}
