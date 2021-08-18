package com.f4sitive.account.entity.generator;

import com.f4sitive.account.util.Snowflakes;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

public class UserIdentifierGenerator implements IdentifierGenerator, Configurable {
    private String entityName;
    private Snowflakes snowflakes;
    private static final String PREFIX = "";

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object obj) throws HibernateException {
        return Optional.ofNullable(session.getEntityPersister(entityName, obj).getIdentifier(obj, session))
                .orElseGet(() -> generate(snowflakes));
    }

    Serializable generate(Snowflakes snowflakes) {
        return Optional.ofNullable(snowflakes)
                .map(Snowflakes::generate).map(Snowflakes::uuid).map(UUID::toString)
                .orElseThrow(() -> new IdentifierGenerationException("ids for this class must be manually assigned before calling save(): " + entityName));
    }

    Serializable test(Snowflakes snowflakes) {
        return Optional.ofNullable(snowflakes)
                .map(this::toString)
                .orElseThrow(() -> new IdentifierGenerationException("ids for this class must be manually assigned before calling save(): " + entityName));
    }

    String toString(Snowflakes snowflakes) {
        String id = Long.toString(snowflakes.generate(), Character.MAX_RADIX);
        id = PREFIX + Integer.toString(id.length(), Character.MAX_RADIX) + id + Long.toHexString(Double.doubleToLongBits(Math.random()));
        return id.substring(0, 20).toUpperCase();
    }

    public static long id(String id) {
        return Long.parseLong(id.substring(PREFIX.length() + 1, PREFIX.length() + 1 + Integer.parseInt(id.substring(PREFIX.length(), PREFIX.length() + 1), Character.MAX_RADIX)), Character.MAX_RADIX);
    }

    @Override
    public void configure(Type type, Properties params, ServiceRegistry serviceRegistry) throws MappingException {
        snowflakes = Optional.ofNullable(serviceRegistry.getService(ConfigurationService.class))
                .map(service -> service.getSetting("snowflakes", Snowflakes.class, null))
                .orElseThrow(() -> new MappingException("no snowflakes"));
        entityName = params.getProperty(ENTITY_NAME);
        if (entityName == null) {
            throw new MappingException("no entity name");
        }
    }
}
