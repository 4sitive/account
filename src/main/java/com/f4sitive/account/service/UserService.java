package com.f4sitive.account.service;

import com.f4sitive.account.entity.User;
import com.f4sitive.account.model.UserDetail;
import com.f4sitive.account.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.Optional;

@Service
public class UserService extends JdbcUserDetailsManager {
    private final UserRepository userRepository;

    public UserService(DataSource dataSource, UserRepository userRepository) {
        super(dataSource);
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void updateUser(UserDetails userDetails) {
        if (userDetails instanceof UserDetail) {
            String registrationId = ((UserDetail) userDetails).getRegistrationId();
            User user = userRepository.queryByUsernameAndRegistrationId(userDetails.getUsername(), registrationId)
                    .orElseGet(() -> new User(userDetails.getUsername(), registrationId));
            Optional.ofNullable(((UserDetail) userDetails).getParentId())
                    .flatMap(userRepository::findById)
                    .filter(parent -> !parent.equals(user))
                    .ifPresent(user::setParent);
            String userId = userRepository.save(user).getId();
            ((UserDetail) userDetails).setId(userId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.queryByUsernameAndRegistrationIdIsNull(username)
                .map(user -> org.springframework.security.core.userdetails.User.builder()
                        .username(user.getId())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException(this.messages.getMessage("JdbcDaoImpl.notFound", new Object[]{username}, "Username {0} not found")));
    }
}
