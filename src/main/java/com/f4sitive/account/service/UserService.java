package com.f4sitive.account.service;

import com.f4sitive.account.entity.AuthorizedClient;
import com.f4sitive.account.entity.User;
import com.f4sitive.account.repository.AuthorizedClientRepository;
import com.f4sitive.account.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsManager {
    private final AuthorizedClientService authorizedClientService;
    private final AuthorizedClientRepository authorizedClientRepository;
    private final UserRepository userRepository;

    @Transactional
    public User findUserByAuthorizedClient(String registrationId, String username) {
//        Optional.ofNullable(authorizedClientService.loadAuthorizedClient(registrationId, username))
//                .map(authorizedClient -> userRepository.findById(((OAuth2AuthorizedClient) authorizedClient).getPrincipalName()))
//                .orElseGet(() -> )
        return authorizedClientRepository.queryByRegistrationIdAndUserUsername(registrationId, username)
                .map(AuthorizedClient::getUser)
                .orElseGet(() -> userRepository.save(User.of(username)));
    }

    @Override
    public void createUser(UserDetails user) {
//        user.get
    }

    @Override
    public void updateUser(UserDetails user) {
//        user.get

    }

    @Override
    public void deleteUser(String username) {

    }

    @Override
    public void changePassword(String oldPassword, String newPassword) {

    }

    @Override
    public boolean userExists(String username) {
        return false;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return null;
    }
}
