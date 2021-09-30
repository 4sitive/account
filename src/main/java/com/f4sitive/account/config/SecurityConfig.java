package com.f4sitive.account.config;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.server.authorization.config.ProviderSettings;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.CookieRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .requestMatcher(request -> !EndpointRequest.toAnyEndpoint().matches(request) && !PathRequest.toStaticResources().atCommonLocations().matches(request))
                .authorizeRequests(customizer -> customizer
                        .requestMatchers(request -> !request.isSecure() && (new AntPathRequestMatcher("/internal/**").matches(request) || new AntPathRequestMatcher("/swagger-ui/**").matches(request) || new AntPathRequestMatcher("/swagger-resources/**").matches(request) || new AntPathRequestMatcher("/v*/api-docs").matches(request))).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(customizer -> customizer
                        .successHandler(successHandler())
                        .failureHandler(failureHandler())
                        .authorizationEndpoint(authorizationEndpointConfig -> authorizationEndpointConfig
                                .authorizationRequestResolver(authorizationRequestResolver()))
                        .tokenEndpoint(tokenEndpointConfig -> tokenEndpointConfig
                                .accessTokenResponseClient(accessTokenResponseClient()))
                        .userInfoEndpoint(userInfoEndpointConfig -> userInfoEndpointConfig
                                .oidcUserService(getApplicationContext().getBean(OAuth2UserService.class))
                                .userService(getApplicationContext().getBean(OAuth2UserService.class)))
                )
                .oauth2Client(Customizer.withDefaults());
    }

    AuthenticationFailureHandler failureHandler() {
        return new SimpleUrlAuthenticationFailureHandler() {
            @Override
            public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
                logger.error(exception);
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
    }

    OAuth2AuthorizationRequestResolver authorizationRequestResolver() {
        RequestCache requestCache = getApplicationContext().getBean(RequestCache.class);
        UserDetailsManager userDetailsManager = getApplicationContext().getBean(UserDetailsManager.class);

        DefaultOAuth2AuthorizationRequestResolver defaultOAuth2AuthorizationRequestResolver = new DefaultOAuth2AuthorizationRequestResolver(getApplicationContext().getBean(ClientRegistrationRepository.class), OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);
        defaultOAuth2AuthorizationRequestResolver.setAuthorizationRequestCustomizer(customizer -> {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            OAuth2AuthorizationRequest authorizationRequest = customizer.build();
            String registrationId = authorizationRequest.getAttribute(OAuth2ParameterNames.REGISTRATION_ID);
            customizer.additionalParameters(additionalParametersConsumer -> {
                switch (registrationId) {
                    case "tid":
                        String prompt = ServletRequestUtils.getStringParameter(request, "prompt", "login");
                        if ("mypage".equals(prompt)) {
                            additionalParametersConsumer.put("login_hint", UriComponentsBuilder.fromHttpUrl("")
                                    .queryParam("login_id", Optional.ofNullable(request.getHeader("User-Id"))
                                            .filter(userId -> userDetailsManager.userExists(userId))
                                            .orElse(""))
                                    .toUriString());
                        }
                        additionalParametersConsumer.put("prompt", prompt);
                        additionalParametersConsumer.put("acr_values", ServletRequestUtils.getStringParameter(request, "acr_values", "login"));
                        additionalParametersConsumer.put("display", ServletRequestUtils.getStringParameter(request, "display", "page"));
                        Optional.ofNullable(ServletRequestUtils.getStringParameter(request, "response_mode", null))
                                .ifPresent(response_mode -> additionalParametersConsumer.put("response_mode", response_mode));
                        Optional.ofNullable(ServletRequestUtils.getStringParameter(request, "login_hint", null))
                                .ifPresent(login_hint -> additionalParametersConsumer.put("login_hint", login_hint));
                        Optional.ofNullable(ServletRequestUtils.getStringParameter(request, "device_info", null))
                                .ifPresent(device_info -> additionalParametersConsumer.put("device_info", device_info));
                        break;
                    case "microsoft":
                        additionalParametersConsumer.put("prompt", ServletRequestUtils.getStringParameter(request, "prompt", "login"));
                        break;
                    case "google":
                        additionalParametersConsumer.put("approval_prompt", ServletRequestUtils.getStringParameter(request, "approval_prompt", "force"));
                        additionalParametersConsumer.put("access_type", ServletRequestUtils.getStringParameter(request, "access_type", "offline"));
                        break;
                }
            });
            customizer.attributes(attributesConsumer -> {
                Optional.ofNullable(request.getHeader("User-Id")).ifPresent(userId -> attributesConsumer.put("username", userId));
                switch (registrationId) {
                    case "tid":
                        String redirectUrl = Optional.ofNullable(requestCache.getRequest(request, null))
                                .map(SavedRequest::getRedirectUrl)
                                .map(UriComponentsBuilder::fromUriString)
                                .map(builder -> builder
                                        .replaceQueryParam("access_token", Collections.emptySet())
                                        .build(false)
                                        .toUriString())
                                .orElseGet(() -> ServletRequestUtils.getStringParameter(request, "url", null));
                        attributesConsumer.put("url", redirectUrl);
                        break;
                    default:
                        attributesConsumer.put("url", ServletRequestUtils.getStringParameter(request, "url", null));
                        break;
                }
            });
        });
        return defaultOAuth2AuthorizationRequestResolver;
    }

    AuthenticationSuccessHandler successHandler() {
        return new SimpleUrlAuthenticationSuccessHandler() {
            @Override
            protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
                if (authentication instanceof OAuth2AuthenticationToken) {
                    String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
                    return "/login/oauth2/success/" + registrationId;
                }
                return super.determineTargetUrl(request, response, authentication);
            }
        };
    }

    OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
        RestTemplate restTemplate = getApplicationContext().getBean(RestTemplateBuilder.class)
                .messageConverters(new FormHttpMessageConverter(), new OAuth2AccessTokenResponseHttpMessageConverter())
                .errorHandler(new OAuth2ErrorResponseErrorHandler())
                .build();
        DefaultAuthorizationCodeTokenResponseClient defaultAuthorizationCodeTokenResponseClient = new DefaultAuthorizationCodeTokenResponseClient();
        defaultAuthorizationCodeTokenResponseClient.setRestOperations(restTemplate);
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
                        providerSettings.getAuthorizationEndpoint(),
                        HttpMethod.GET.name()),
                new AntPathRequestMatcher(
                        providerSettings.getAuthorizationEndpoint(),
                        HttpMethod.POST.name())));
        return requestCache;
    }
}
