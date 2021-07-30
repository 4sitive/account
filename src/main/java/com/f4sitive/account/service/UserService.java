package com.f4sitive.account.service;

import com.f4sitive.account.entity.User;
import com.f4sitive.account.repository.UserRepository;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;

@Service
public class UserService extends JdbcUserDetailsManager {
    private final UserRepository userRepository;
    private final MongoOperations mongoOperations;

    public UserService(DataSource dataSource,
                       UserRepository userRepository, MongoOperations mongoOperations) {
        super(dataSource);
        this.userRepository = userRepository;
//        setCreateUserSql(JdbcUserDetailsManager.DEF_CREATE_USER_SQL.replaceAll("users", "user"));
//        setDeleteUserSql(JdbcUserDetailsManager.DEF_DELETE_USER_SQL.replaceAll("users", "user"));
//        setUpdateUserSql(JdbcUserDetailsManager.DEF_UPDATE_USER_SQL.replaceAll("users", "user"));
//        setUserExistsSql(JdbcUserDetailsManager.DEF_USER_EXISTS_SQL.replaceAll("users", "user"));
//        setChangePasswordSql(JdbcUserDetailsManager.DEF_CHANGE_PASSWORD_SQL.replaceAll("users", "user"));
//        setFindAllGroupsSql(JdbcUserDetailsManager.DEF_FIND_GROUPS_SQL.replaceAll("groups", "group"));
//        setFindUsersInGroupSql(JdbcUserDetailsManager.DEF_FIND_USERS_IN_GROUP_SQL.replaceAll("groups", "group"));
//        setInsertGroupSql(JdbcUserDetailsManager.DEF_INSERT_GROUP_SQL.replaceAll("groups", "group"));
//        setFindGroupIdSql(JdbcUserDetailsManager.DEF_FIND_GROUP_ID_SQL.replaceAll("groups", "group"));
//        setDeleteGroupSql(JdbcUserDetailsManager.DEF_DELETE_GROUP_SQL.replaceAll("groups", "group"));
//        setRenameGroupSql(JdbcUserDetailsManager.DEF_RENAME_GROUP_SQL.replaceAll("groups", "group"));
//        setGroupAuthoritiesSql(JdbcUserDetailsManager.DEF_GROUP_AUTHORITIES_QUERY_SQL.replaceAll("groups", "group"));
        this.mongoOperations = mongoOperations;
    }

    @Override
    @Transactional
    public void createGroup(String groupName, List<GrantedAuthority> authorities) {
        super.createGroup(groupName, authorities);
    }

    @Transactional
    public String findUserIdByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(User::getId)
                .orElseGet(() -> {
                    User user = User.of(org.springframework.security.core.userdetails.User.withUsername(username).password(UUID.randomUUID().toString()).authorities(AuthorityUtils.NO_AUTHORITIES).build());
//                    Optional.ofNullable(mongoOperations.findOne(Query.query(Criteria.where("username").is(username)), User.class))
//                            .map(User::getId)
//                            .ifPresent(user::setId);
                    return userRepository.save(user).getId();
//                    createUser(org.springframework.security.core.userdetails.User.withUsername(username).password(UUID.randomUUID().toString()).authorities(AuthorityUtils.NO_AUTHORITIES).build());
//                    return userRepository.findByUsername(username).map(User::getId).orElseThrow(IllegalStateException::new);
                });
    }
    //    private final AuthorizedClientService authorizedClientService;
//    private final AuthorizedClientRepository authorizedClientRepository;
//    private final UserRepository userRepository;
//
//    @Transactional
//    public User findUserByAuthorizedClient(String registrationId, String username) {
////        Optional.ofNullable(authorizedClientService.loadAuthorizedClient(registrationId, username))
////                .map(authorizedClient -> userRepository.findById(((OAuth2AuthorizedClient) authorizedClient).getPrincipalName()))
////                .orElseGet(() -> )
//        return authorizedClientRepository.queryByRegistrationIdAndUserUsername(registrationId, username)
//                .map(AuthorizedClient::getUser)
//                .orElseGet(() -> userRepository.save(User.of(username)));
//    }
}
