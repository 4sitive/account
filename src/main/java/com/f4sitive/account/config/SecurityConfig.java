package com.f4sitive.account.config;

import com.f4sitive.account.entity.User;
import com.f4sitive.account.service.AuthorizedClientService;
import com.f4sitive.account.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.client.HttpClient;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequestEntityConverter;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.AuthenticatedPrincipalOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.server.authorization.config.ProviderSettings;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.CookieRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
//    private final AuthorizedClientService authorizedClientService;
//
//    public SecurityConfig(AuthorizedClientService authorizedClientService) {
//        this.authorizedClientService = authorizedClientService;
//    }

    //    @Bean
    OAuth2AuthorizedClientRepository authorizedClientRepository(OAuth2AuthorizedClientService authorizedClientService) {
        AuthenticatedPrincipalOAuth2AuthorizedClientRepository authorizedClientRepository = new AuthenticatedPrincipalOAuth2AuthorizedClientRepository(authorizedClientService);
        authorizedClientRepository.setAnonymousAuthorizedClientRepository(new OAuth2AuthorizedClientRepository() {
            @Override
            public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String clientRegistrationId, Authentication principal, HttpServletRequest request) {
                return null;
            }

            @Override
            public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal, HttpServletRequest request, HttpServletResponse response) {
            }

            @Override
            public void removeAuthorizedClient(String clientRegistrationId, Authentication principal, HttpServletRequest request, HttpServletResponse response) {
            }
        });
        return authorizedClientRepository;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        SimpleUrlAuthenticationSuccessHandler successHandler = new SimpleUrlAuthenticationSuccessHandler() {
            @Override
            protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
                if (authentication instanceof OAuth2AuthenticationToken) {
                    String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
                    return "/login/oauth2/success/" + registrationId;
                }
                return super.determineTargetUrl(request, response, authentication);
            }
        };
        SimpleUrlAuthenticationFailureHandler failureHandler = new SimpleUrlAuthenticationFailureHandler() {
            @Override
            public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
                String redirectUrl = Optional.ofNullable(WebApplicationContextUtils.getRequiredWebApplicationContext(request.getServletContext()).getBean(RequestCache.class).getRequest(request, response))
                        .map(SavedRequest::getRedirectUrl)
                        .map(url -> {
                            if (exception instanceof OAuth2AuthenticationException) {
                                return UriComponentsBuilder.fromUriString(url)
                                        .queryParam(OAuth2ParameterNames.ERROR, ((OAuth2AuthenticationException) exception).getError().getErrorCode())
                                        .build(false)
                                        .toUriString();
                            } else {
                                return url;
                            }
                        })
                        .orElse(null);
                if (StringUtils.hasText(redirectUrl)) {
                    getRedirectStrategy().sendRedirect(request, response, redirectUrl);
                } else {
                    super.onAuthenticationFailure(request, response, exception);
                }
            }
        };
//Endpoint
//        new NegatedRequestMatcher(EndpointRequest.toAnyEndpoint())
//        new AntPathRequestMatcher("/oauth2/authorization/*", null, false),
//                new AntPathRequestMatcher("/login", null, false),
//                new AntPathRequestMatcher(OAuth2LoginAuthenticationFilter.DEFAULT_FILTER_PROCESSES_URI + ".code", null, false),
        http
                .requestMatcher(new NegatedRequestMatcher(EndpointRequest.toAnyEndpoint()))
                .authorizeRequests(requests -> requests.anyRequest().authenticated())
                .oauth2Login(customizer -> customizer
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .tokenEndpoint(tokenEndpointConfig -> tokenEndpointConfig
                                .accessTokenResponseClient(accessTokenResponseClient(getApplicationContext().getBean(HttpClient.class)))
                        )
                        .userInfoEndpoint(userInfoEndpointConfig -> userInfoEndpointConfig
                                .userService(userService(getApplicationContext().getBean(HttpClient.class))))
                )
                .oauth2Client(Customizer.withDefaults());
    }

    public WebSecurityCustomizer ss() {
        return new WebSecurityCustomizer() {
            @Override
            public void customize(WebSecurity web) {
//                web.
            }
        };
    }

    private OAuth2UserService<OAuth2UserRequest, OAuth2User> userService(HttpClient httpClient) {
        RestTemplate restTemplate = new RestTemplateBuilder()
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(httpClient))
                .errorHandler(new OAuth2ErrorResponseErrorHandler())
                .build();
        Converter<OAuth2UserRequest, RequestEntity<?>> requestEntityConverter = new OAuth2UserRequestEntityConverter();
        UserService userService = getApplicationContext().getBean(UserService.class);
//        DefaultOAuth2UserService userService = new DefaultOAuth2UserService();
//        userService.setRestOperations(restTemplate);
//        return userService;
        return userRequest -> {
            Assert.notNull(userRequest, "userRequest cannot be null");
//            userRequest.get
            String registrationId = userRequest.getClientRegistration().getRegistrationId();
            RequestEntity<?> request = requestEntityConverter.convert(userRequest);
            Map<String, Object> attributes = new LinkedHashMap<>();
            String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
            try {
                ResponseEntity<JsonNode> response = restTemplate.exchange(request, JsonNode.class);
                JsonNode body = response.getBody();
                switch (registrationId) {
                    case "naver":
                        attributes.put("id", body.with("response").get(userNameAttributeName).asText());
                        break;
                    default:
                        attributes.put("id", body.get(userNameAttributeName).asText());
                        break;
                }
            } catch (RestClientException ex) {
                OAuth2Error oauth2Error = new OAuth2Error("invalid_user_info_response", "An error occurred while attempting to retrieve the UserInfo Resource: " + ex.getMessage(), null);
                throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString(), ex);
            }
            attributes.putAll(userRequest.getAdditionalParameters());
            String id = Optional.ofNullable((String) userRequest.getAdditionalParameters().get("username"))
                    .orElseGet(() -> registrationId + "_" + attributes.get("id"));
            return new DefaultOAuth2User(AuthorityUtils.NO_AUTHORITIES, Collections.singletonMap("id", userService.findUserIdByUsername(id)), "id");
        };
    }

    private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient(HttpClient httpClient) {
        RestTemplate restTemplate = new RestTemplateBuilder()
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(httpClient))
                .messageConverters(new FormHttpMessageConverter(), new OAuth2AccessTokenResponseHttpMessageConverter())
                .errorHandler(new OAuth2ErrorResponseErrorHandler())
                .build();
        DefaultAuthorizationCodeTokenResponseClient defaultAuthorizationCodeTokenResponseClient = new DefaultAuthorizationCodeTokenResponseClient();
        defaultAuthorizationCodeTokenResponseClient.setRestOperations(restTemplate);
//        defaultAuthorizationCodeTokenResponseClient.setRequestEntityConverter(oAuth2AuthorizationCodeGrantRequestEntityConverter());
        return authorizationGrantRequest -> {
            OAuth2AccessTokenResponse tokenResponse = defaultAuthorizationCodeTokenResponseClient.getTokenResponse(authorizationGrantRequest);
            Map<String, Object> additionalParameters = new LinkedHashMap<>(tokenResponse.getAdditionalParameters());
            additionalParameters.put("username", authorizationGrantRequest.getAuthorizationExchange().getAuthorizationRequest().getAttributes().get("username"));
            additionalParameters.put("url", authorizationGrantRequest.getAuthorizationExchange().getAuthorizationRequest().getAttributes().get("url"));
            return OAuth2AccessTokenResponse.withResponse(tokenResponse)
                    .additionalParameters(additionalParameters)
                    .build();
        };
    }

    @Bean
    RequestCache requestCache(ProviderSettings providerSettings) {
        CookieRequestCache requestCache = new CookieRequestCache();
        requestCache.setRequestMatcher(new OrRequestMatcher(
                new AntPathRequestMatcher(
                        providerSettings.authorizationEndpoint(),
                        HttpMethod.GET.name()),
                new AntPathRequestMatcher(
                        providerSettings.authorizationEndpoint(),
                        HttpMethod.POST.name())));
        return requestCache;
    }
}
