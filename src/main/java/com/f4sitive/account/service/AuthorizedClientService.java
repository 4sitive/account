package com.f4sitive.account.service;

import com.f4sitive.account.entity.AuthorizedClient;
import com.f4sitive.account.entity.User;
import com.f4sitive.account.repository.AuthorizedClientRepository;
import com.f4sitive.account.repository.UserRepository;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

@Service
public class AuthorizedClientService implements OAuth2AuthorizedClientService {
    private final AuthorizedClientRepository authorizedClientRepository;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final UserRepository userRepository;

    public AuthorizedClientService(AuthorizedClientRepository authorizedClientRepository,
                                   ClientRegistrationRepository clientRegistrationRepository,
                                   UserRepository userRepository) {
        this.authorizedClientRepository = authorizedClientRepository;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public User findUserByAuthorizedClient(String registrationId, String username) {
        return authorizedClientRepository.queryByRegistrationIdAndUserUsername(registrationId, username)
                .map(AuthorizedClient::getUser)
                .orElseGet(() -> userRepository.save(User.of(username)));
    }

    @Override
    public OAuth2AuthorizedClient loadAuthorizedClient(String clientRegistrationId, String principalName) {
        return authorizedClientRepository.queryByRegistrationIdAndUserId(clientRegistrationId, principalName)
                .map(authorizedClient -> {
                    ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(authorizedClient.getRegistrationId());
                    if (clientRegistration == null) {
                        throw new DataRetrievalFailureException("The ClientRegistration with id '" + clientRegistrationId + "' exists in the data source, " + "however, it was not found in the ClientRegistrationRepository.");
                    }
                    OAuth2AccessToken.TokenType tokenType = null;
                    if (OAuth2AccessToken.TokenType.BEARER.getValue().equalsIgnoreCase(authorizedClient.getAccessTokenType())) {
                        tokenType = OAuth2AccessToken.TokenType.BEARER;
                    }
                    String tokenValue = authorizedClient.getAccessToken();
                    Instant issuedAt = authorizedClient.getAccessTokenIssuedAt();
                    Instant expiresAt = authorizedClient.getAccessTokenExpiresAt();
                    Set<String> scopes = authorizedClient.getAccessTokenScopes();
                    OAuth2AccessToken accessToken = new OAuth2AccessToken(tokenType, tokenValue, issuedAt, expiresAt, scopes);
                    OAuth2RefreshToken refreshToken = Optional.ofNullable(authorizedClient.getRefreshToken())
                            .map(token -> new OAuth2RefreshToken(token, authorizedClient.getRefreshTokenIssuedAt()))
                            .orElse(null);
                    return new OAuth2AuthorizedClient(clientRegistration, authorizedClient.getUser().getId(), accessToken, refreshToken);
                })
                .orElse(null);
    }

    @Transactional
    @Override
    public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal) {
        authorizedClientRepository.queryByRegistrationIdAndUserId(authorizedClient.getClientRegistration().getRegistrationId(), principal.getName())
                .map(it -> {
                    it.setAccessToken(authorizedClient.getAccessToken().getTokenValue());
                    it.setAccessTokenExpiresAt(authorizedClient.getAccessToken().getExpiresAt());
                    it.setAccessTokenIssuedAt(authorizedClient.getAccessToken().getIssuedAt());
                    it.setAccessTokenType(authorizedClient.getAccessToken().getTokenType().getValue());
                    it.setAccessTokenScopes(authorizedClient.getAccessToken().getScopes());
                    Optional.ofNullable(authorizedClient.getRefreshToken())
                            .ifPresent(token -> {
                                it.setRefreshToken(token.getTokenValue());
                                it.setRefreshTokenIssuedAt(token.getIssuedAt());
                            });
                    return it;
                })
                .orElseGet(() -> {
                    AuthorizedClient it = new AuthorizedClient();
                    it.setRegistrationId(authorizedClient.getClientRegistration().getRegistrationId());
                    it.setUser(new User(principal.getName()));
                    it.setAccessToken(authorizedClient.getAccessToken().getTokenValue());
                    it.setAccessTokenExpiresAt(authorizedClient.getAccessToken().getExpiresAt());
                    it.setAccessTokenIssuedAt(authorizedClient.getAccessToken().getIssuedAt());
                    it.setAccessTokenType(authorizedClient.getAccessToken().getTokenType().getValue());
                    it.setAccessTokenScopes(authorizedClient.getAccessToken().getScopes());
                    Optional.ofNullable(authorizedClient.getRefreshToken())
                            .ifPresent(token -> {
                                it.setRefreshToken(token.getTokenValue());
                                it.setRefreshTokenIssuedAt(token.getIssuedAt());
                            });
                    return authorizedClientRepository.save(it);
                });
    }

    @Transactional
    @Override
    public void removeAuthorizedClient(String clientRegistrationId, String principalName) {
        authorizedClientRepository.queryByRegistrationIdAndUserId(clientRegistrationId, principalName)
                .ifPresent(authorizedClientRepository::delete);
    }
}
