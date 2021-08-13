package com.f4sitive.account.entity;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQL57Dialect;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import javax.persistence.Entity;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

class SchemaTest {
    @Test
    void test() {
        create(H2Dialect.class);
        create(MySQL57Dialect.class);
    }

    private void create(Class<? extends Dialect> dialect) {
        BootstrapServiceRegistry bootstrapServiceRegistry = new BootstrapServiceRegistryBuilder()
                .disableAutoClose()
                .build();

        StandardServiceRegistry standardServiceRegistry = new StandardServiceRegistryBuilder(bootstrapServiceRegistry)
                .applySetting(AvailableSettings.DIALECT, dialect)
                .applySetting(AvailableSettings.IMPLICIT_NAMING_STRATEGY, SpringImplicitNamingStrategy.class.getName())
                .applySetting(AvailableSettings.PHYSICAL_NAMING_STRATEGY, SpringPhysicalNamingStrategy.class.getName())
                .applySetting(AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, false)
                .applySetting(AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, true)
                .applySetting(AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS_SKIP_COLUMN_DEFINITIONS, true)
                .build();

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        List<String> annotatedClassNames = scanner.findCandidateComponents(getClass().getPackage().getName())
                .stream()
                .map(BeanDefinition::getBeanClassName)
                .collect(Collectors.toList());

        MetadataSources metadataSources = new MetadataSources(standardServiceRegistry);
        annotatedClassNames.stream().forEach(metadataSources::addAnnotatedClassName);

        SchemaExport schemaExport = new SchemaExport();
        schemaExport.setDelimiter(";");
        schemaExport.setFormat(true);
        schemaExport.createOnly(EnumSet.of(TargetType.STDOUT), metadataSources.buildMetadata());
    }
}
