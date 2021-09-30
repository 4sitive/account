package com.f4sitive.account.service;

import com.f4sitive.account.model.UserDetail;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.DelegatingOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequestEntityConverter;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;
import org.springframework.vault.support.JsonMapFlattener;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class OAuth2UserService extends DelegatingOAuth2UserService {
    private final UserDetailsManager userDetailsManager;

    public OAuth2UserService(RestTemplateBuilder restTemplateBuilder, UserDetailsManager userDetailsManager) {
        super(userServices(restTemplateBuilder));
        this.userDetailsManager = userDetailsManager;
    }

    private static List<org.springframework.security.oauth2.client.userinfo.OAuth2UserService> userServices(RestTemplateBuilder restTemplateBuilder) {
        ParameterizedTypeReference<Map<String, Object>> parameterizedTypeReference = new ParameterizedTypeReference<Map<String, Object>>() {
        };
        OAuth2UserRequestEntityConverter requestEntityConverter = oauth2UserRequestEntityConverter();
        RestTemplate restTemplate = restTemplateBuilder.errorHandler(new OAuth2ErrorResponseErrorHandler()).build();
        List<org.springframework.security.oauth2.client.userinfo.OAuth2UserService> userServices = new ArrayList<>();
        userServices.add(new DefaultOAuth2UserService() {
            @Override
            public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
                if (userRequest instanceof OidcUserRequest) {
                    return null;
                } else {
                    try {
                        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(requestEntityConverter.convert(userRequest), parameterizedTypeReference);
                        Map<String, Object> attributes = response.getBody();
                        attributes.put("username", JsonMapFlattener.flattenToStringMap(attributes).get(userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName()));
                        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
                        authorities.add(new OAuth2UserAuthority(attributes));
                        OAuth2AccessToken token = userRequest.getAccessToken();
                        for (String authority : token.getScopes()) {
                            authorities.add(new SimpleGrantedAuthority("SCOPE_" + authority));
                        }
                        return new DefaultOAuth2User(authorities, attributes, "username");
                    } catch (RestClientException ex) {
                        OAuth2Error oauth2Error = new OAuth2Error("invalid_user_info_response", "An error occurred while attempting to retrieve the UserInfo Resource: " + ex.getMessage(), null);
                        throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString(), ex);
                    }
                }
            }
        });
        userServices.add(new OidcUserService() {
            {
                setOauth2UserService(new DefaultOAuth2UserService() {
                    {
                        setRestOperations(restTemplate);
                    }
                });
            }
        });
        return userServices;
    }

    private static OAuth2UserRequestEntityConverter oauth2UserRequestEntityConverter() {
        return new OAuth2UserRequestEntityConverter() {
            @Override
            public RequestEntity<?> convert(OAuth2UserRequest userRequest) {
                RequestEntity<?> convert = super.convert(userRequest);
                return new RequestEntity<>(convert.getBody(), HttpHeaders.writableHttpHeaders(convert.getHeaders()), convert.getMethod(), convert.getUrl());
            }
        };
    }


    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        return Optional.ofNullable(super.loadUser(userRequest))
                .map(loadUser -> {
                    String registrationId = userRequest.getClientRegistration().getRegistrationId();
                    String name = JsonMapFlattener.flattenToStringMap(loadUser.getAttributes()).get("name");
                    UserDetail userDetail = new UserDetail();
                    userDetail.setUsername(loadUser.getName());
                    userDetail.setRegistrationId(registrationId);
                    userDetail.setName(name);
                    Optional.ofNullable(userRequest.getAdditionalParameters().get("user_id"))
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .ifPresent(userDetail::setParentId);
                    userDetailsManager.updateUser(userDetail);
                    Map<String, Object> attributes = new LinkedHashMap<>(loadUser.getAttributes());
                    attributes.put("user_id", userDetail.getId());
                    attributes.put("user_name", name);
                    return loadUser instanceof OidcUser ? new DefaultOidcUser(loadUser.getAuthorities(), new OidcIdToken(((OidcUser) loadUser).getIdToken().getTokenValue(), ((OidcUser) loadUser).getIdToken().getIssuedAt(), ((OidcUser) loadUser).getIdToken().getExpiresAt(), attributes), ((OidcUser) loadUser).getUserInfo(), "user_id") : new DefaultOAuth2User(loadUser.getAuthorities(), attributes, "user_id");
                })
                .orElse(null);
    }

}
