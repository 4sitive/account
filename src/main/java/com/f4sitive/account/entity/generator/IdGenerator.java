package com.f4sitive.account.entity.generator;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.LongType;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.Properties;
import java.util.UUID;

public class IdGenerator extends SequenceStyleGenerator {
    @Override
    public void configure(Type type, Properties params, ServiceRegistry serviceRegistry) throws MappingException {
        super.configure(LongType.INSTANCE, params, serviceRegistry);
    }

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
        Serializable id = super.generate(session, object);
        if(id instanceof Number){
            id = (Long.toString(System.currentTimeMillis(), 36).toUpperCase() + Long.toString(((Number) id).longValue(), 36).toUpperCase() + UUID.randomUUID().toString().replace("-", "").toUpperCase()).codePoints().limit(20).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
        }
        return id;
    }
}
