package com.f4sitive.account.entity.listener;

import com.f4sitive.account.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.util.Assert;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import javax.persistence.PostPersist;
import javax.persistence.PrePersist;
import java.util.Optional;

@Configurable
public class UserEntityListener {
    @Autowired
    private MongoOperations bean;

    public UserEntityListener() {
        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
    }

    @PrePersist
    public void prePersist(User target) {
        Optional.ofNullable(bean)
                .flatMap(mongoOperations -> Optional.ofNullable(mongoOperations.findOne(Query.query(Criteria.where("username").is(target.getRegistrationId() + "_" + target.getUsername())), User.class)))
                .map(User::getId)
                .ifPresent(target::setId);
    }

    @PostPersist
    public void postPersist(User target) {
        Assert.notNull(target, "Entity must not be null!");
        Optional.ofNullable(bean)
                .ifPresent(mongoOperations -> mongoOperations.findAndModify(
                        Query.query(Criteria.where("id").is(target.getId())),
                        Update.update("username", target.getRegistrationId() + "_" + target.getUsername()),
                        FindAndModifyOptions.options().upsert(true),
                        User.class
                ));
    }
}
