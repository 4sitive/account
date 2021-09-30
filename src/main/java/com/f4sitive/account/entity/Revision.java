package com.f4sitive.account.entity;

import com.f4sitive.account.util.Snowflakes;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.envers.ModifiedEntityNames;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.domain.Persistable;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Table;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@ToString(callSuper = false, onlyExplicitlyIncluded = true)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@NoArgsConstructor
@RevisionEntity
@Entity
@Table(name = "revision")
public class Revision implements Persistable<Long> {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = Constants.SNOWFLAKE_IDENTIFIER_GENERATOR)
    @GenericGenerator(name = Constants.SNOWFLAKE_IDENTIFIER_GENERATOR, strategy = Constants.SNOWFLAKE_IDENTIFIER_GENERATOR_STRATEGY)
    @RevisionNumber
    @ToString.Include
    @EqualsAndHashCode.Include
    private Long id;

    @ModifiedEntityNames
    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "revision_modified_entity", joinColumns = @JoinColumn(foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT)))
    @Column(length = 200)
    private Set<String> name = new LinkedHashSet<>();

    @CreatedDate
    @Access(AccessType.PROPERTY)
    @RevisionTimestamp
    @ToString.Include
    @EqualsAndHashCode.Include
    private Date createdDate;

    @CreatedBy
    @Access(AccessType.PROPERTY)
    @Column(length = Constants.ID_LENGTH)
    @ToString.Include
    private String createdBy;

    public Long id(Snowflakes snowflakes) {
        return snowflakes.generate();
    }

    @Override
    public boolean isNew() {
        return getId() == null;
    }
}