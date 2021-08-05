package com.f4sitive.account.entity;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Getter
@Setter
@Audited
@Table
@Entity
public class Test {
    @Id
    private String id;
}
