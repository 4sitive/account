package com.f4sitive.account.entity.generator;

import com.f4sitive.account.util.Snowflakes;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.Optional;
import java.util.Properties;

public class UserIdentifierGenerator implements IdentifierGenerator, Configurable {
    private String entityName;
    private Snowflakes snowflakes;

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object obj) throws HibernateException {
        final Serializable id = session.getEntityPersister(entityName, obj).getIdentifier(obj, session);
        return id == null ? generate() : id;
    }

    Serializable generate() {
        return Snowflakes.uuid(snowflakes.generate()).toString();
//        return Optional.ofNullable(snowflakes)
//                .map(Snowflakes::generate)
//                .map(Snowflakes::uuid)
//                .map(UUID::toString)
//                .orElseThrow(() -> new IdentifierGenerationException("ids for this class must be manually assigned before calling save(): " + entityName));
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
