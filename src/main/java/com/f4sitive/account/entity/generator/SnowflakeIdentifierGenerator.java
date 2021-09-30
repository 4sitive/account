package com.f4sitive.account.entity.generator;

import com.f4sitive.account.util.Snowflakes;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;
import org.springframework.util.ReflectionUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Properties;

public class SnowflakeIdentifierGenerator implements org.hibernate.id.IdentifierGenerator, Configurable {
    private String entityName;
    private Snowflakes snowflakes;
    private Method method;

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object obj) throws HibernateException {
        return Optional.ofNullable(session.getEntityPersister(entityName, obj).getIdentifier(obj, session))
                .orElseGet(() -> (Serializable) ReflectionUtils.invokeMethod(method, obj, snowflakes));
    }

    @Override
    public void configure(Type type, Properties params, ServiceRegistry serviceRegistry) throws MappingException {
        snowflakes = Optional.ofNullable(serviceRegistry.getService(ConfigurationService.class))
                .map(service -> service.getSetting("snowflakes", Snowflakes.class, null))
                .orElseThrow(() -> new MappingException("no snowflakes"));
        entityName = Optional.ofNullable(params.getProperty(ENTITY_NAME))
                .orElseThrow(() -> new MappingException("no entity name"));
        method = ReflectionUtils.findMethod(
                serviceRegistry.getService(ClassLoaderService.class).classForName(entityName),
                params.getProperty(PersistentIdentifierGenerator.PK),
                Snowflakes.class
        );
    }
}
