package com.f4sitive.account.entity;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

@Getter
@Setter
@Audited
@Table
@Entity
public class Test {
    @Id
    @GeneratedValue(generator = "Generator")
    @GenericGenerator(
            name = "Generator",
//            strategy = "org.hibernate.envers.enhanced.OrderedSequenceGenerator",
//            strategy = "com.f4sitive.account.entity.generator.IdGenerator",
            strategy = "com.f4sitive.account.entity.generator.SnowballGenerator",
            parameters = {
//                    @org.hibernate.annotations.Parameter(name = "table_name", value = "REVISION_GENERATOR"),
//                    @org.hibernate.annotations.Parameter(name = "sequence_name", value = "REVISION_GENERATOR"),
//                    @org.hibernate.annotations.Parameter(name = "initial_value", value = "1"),
                    @org.hibernate.annotations.Parameter(name = "increment_size", value = "1"),
                    @org.hibernate.annotations.Parameter(name = "force_table_use", value = "true"),
                    @org.hibernate.annotations.Parameter(name = "value_column", value = "id")
            }
    )
//    @TableGenerator()
    private String id;
}
