package com.f4sitive.account.entity;

import com.f4sitive.account.converter.SetToCommaDelimitedStringConverter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.envers.ModifiedEntityNames;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.domain.Persistable;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@ToString(callSuper = false, of = {"id"})
@EqualsAndHashCode(callSuper = false, of = {"id"})
@RevisionEntity
@Entity
@Table
public class Revision implements Persistable<Long> {
    private static final long serialVersionUID = 845947266662871674L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @RevisionNumber
    @Column
    private Long id;

    @ModifiedEntityNames
//    @Convert(converter = SetToCommaDelimitedStringConverter.class)
//    @Lob
//    @Basic
    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "revision_modified_entity",
            joinColumns = @JoinColumn(foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
    )
    private Set<String> name = new HashSet<>();

    @CreatedDate
    @Access(AccessType.PROPERTY)
    @RevisionTimestamp
    @Column(updatable = false)
    private Date createdDate;

    @CreatedBy
    @Access(AccessType.PROPERTY)
    @Column(length = 20, updatable = false)
    private String createdBy;

    @Override
    public boolean isNew() {
        return getId() == null;
    }
}