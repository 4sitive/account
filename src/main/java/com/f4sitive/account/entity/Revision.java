package com.f4sitive.account.entity;

import com.f4sitive.account.converter.StringSetToCommaDelimitedStringConverter;
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
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@ToString(callSuper = false, of = { "id" })
@EqualsAndHashCode(callSuper = false, of = { "id" })
@RevisionEntity
@Entity
@Table(name = "REVISION")
public class Revision implements Persistable<Long> {
    private static final long serialVersionUID = 845947266662871674L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @RevisionNumber
    @Column(name = "ID")
    private Long id;

    @Convert(converter = StringSetToCommaDelimitedStringConverter.class)
    @Column(name = "MODIFIED_ENTITY_NAMES", length = 150, nullable = false, updatable = false)
    @ModifiedEntityNames
    @Lob
    private Set<String> modifiedEntityNames = new HashSet<>();

    @CreatedDate
    @Access(AccessType.PROPERTY)
    @RevisionTimestamp
    @Column(name = "CREATED_DATE", updatable = false)
    private Date createdDate;

    @CreatedBy
    @Access(AccessType.PROPERTY)
    @Column(name = "CREATED_BY", length = 20, updatable = false)
    private String createdBy;

    @Override
    public boolean isNew() {
        return getId() == null;
    }
}