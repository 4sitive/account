package com.f4sitive.account.entity.listener;

import com.f4sitive.account.entity.User;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.util.Assert;

import javax.persistence.PostPersist;
import javax.persistence.PrePersist;
import java.util.Optional;

public class UserEntityListener {
    private final Optional<MongoOperations> mongoOperations;

    public UserEntityListener(Optional<MongoOperations> mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @PrePersist
    public void prePersist(User target) {
//            Query.query(Criteria.where("username").is(target.getUsername())), User.class)
        mongoOperations.flatMap(m -> Optional.ofNullable(m.findOne(Query.query(Criteria.where("username").is(target.getUsername())), User.class)))
                .map(User::getId)
                .ifPresent(target::setId);
//            Optional.ofNullable(mongoOperations.findOne(Query.query(Criteria.where("username").is(target.getUsername())), User.class))
//                    .map(User::getId)
//                    .ifPresent(target::setId);
    }

    @PostPersist
    public void postPersist(User target) {
        Assert.notNull(target, "Entity must not be null!");
        mongoOperations.ifPresent(m -> {
            m.findAndModify(
                    Query.query(Criteria.where("_id").is(target.getId())),
                    Update.update("username", target.getUsername()),
                    FindAndModifyOptions.options().upsert(true),
                    User.class
            );
        });
//            ,
//            FindAndModifyOptions.options().upsert(true),
//            mongoOperations.findAndModify(
//                    Query.query(Criteria.where("_id").is(target.getId())),
//                    Update.update("username", target.getUsername()),
//                    FindAndModifyOptions.options().upsert(true),
//                    User.class
//            );
    }
}
