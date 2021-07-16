package com.f4sitive.account.entity;

import javax.persistence.*;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table
public class Group {
    @Id
    private Integer id;

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "group_authorities", joinColumns = @JoinColumn(foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT)))
//    @Column(name = "authority", nullable = false)
    private Set<String> authority = new LinkedHashSet<>();

    @ManyToMany
    @ElementCollection(fetch = FetchType.EAGER)
//    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name="group_members",
            joinColumns=@JoinColumn(foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT)),
            foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT)
    )
//    @JoinTable(name = "group_members", joinColumns = @JoinColumn(foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT)))
//    @Column(name = "user_id", nullable = false)
    private List<User> user;
}
