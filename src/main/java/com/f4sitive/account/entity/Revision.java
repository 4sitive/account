package com.f4sitive.account.entity;

import com.f4sitive.account.converter.SetToCommaDelimitedStringConverter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.envers.*;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.domain.Persistable;

import javax.persistence.*;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@ToString(callSuper = false, of = {"id"})
@EqualsAndHashCode(callSuper = false, of = {"id"})
@RevisionEntity
@Entity
@Table
public class Revision implements Persistable<Long> {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(generator = "RevisionNumberSequenceGenerator")
    @GenericGenerator(
            name = "RevisionNumberSequenceGenerator",
            strategy = "org.hibernate.envers.enhanced.OrderedSequenceGenerator",
            parameters = {
                    @org.hibernate.annotations.Parameter(name = "table_name", value = "REVISION_GENERATOR"),
                    @org.hibernate.annotations.Parameter(name = "sequence_name", value = "REVISION_GENERATOR"),
                    @org.hibernate.annotations.Parameter(name = "initial_value", value = "1"),
                    @org.hibernate.annotations.Parameter(name = "increment_size", value = "1")
            }
    )
    @RevisionNumber
    private Long id;

    @ModifiedEntityNames
    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "revision_modified_entity",
            joinColumns = @JoinColumn(foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
    )
    @Column(length = 200)
    private Set<String> name = new LinkedHashSet<>();

    @CreatedDate
    @Access(AccessType.PROPERTY)
    @RevisionTimestamp
    private Date createdDate;

    @CreatedBy
    @Access(AccessType.PROPERTY)
    @Column(length = User.ID_LENGTH)
    private String createdBy;

    @Override
    public boolean isNew() {
        return getId() == null;
    }
}