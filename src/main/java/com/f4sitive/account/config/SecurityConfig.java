package com.f4sitive.account.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.CookieRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        SimpleUrlAuthenticationSuccessHandler successHandler = new SimpleUrlAuthenticationSuccessHandler() {
            @Override
            protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
                if (authentication instanceof OAuth2AuthenticationToken) {
                    String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
                    return "/login/oauth2/confirm/" + registrationId;
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

        http
                .authorizeRequests((requests) -> requests.anyRequest().authenticated())
                .oauth2Login(customizer -> customizer.successHandler(successHandler).failureHandler(failureHandler));//Customizer.withDefaults()
    }

    @Bean
    public RequestCache requestCache() {
        return new CookieRequestCache();
    }
}
