package com.f4sitive.account;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.provisioning.GroupManager;
import org.springframework.security.provisioning.UserDetailsManager;

import java.time.Duration;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    BeanPostProcessor beanPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof RegisteredClientRepository) {
                    ((RegisteredClientRepository) bean).save(RegisteredClient.withId("4sitive")
                            .clientId("4sitive")
                            .clientSecret("{noop}secret")
                            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                            .redirectUri("http://127.0.0.1/login/oauth2/code/TEST")
                            .redirectUri("https://oauth.pstmn.io/v1/callback")
                            .redirectUri("positive://login")
                            .redirectUri("http://localhost:8080/swagger-ui/oauth2-redirect.html")
                            .redirectUri("http://lvh.me:8080/swagger-ui/oauth2-redirect.html")
                            .redirectUri("https://api.4sitive.com/swagger-ui/oauth2-redirect.html")
                            .scope("message.read")
                            .scope("message.write")
                            .tokenSettings(tokenSettings -> tokenSettings.accessTokenTimeToLive(Duration.ofDays(1L)))
                            .build());
                }
                if (bean instanceof UserDetailsManager && bean instanceof GroupManager) {
                    ((GroupManager) bean).createGroup("test", AuthorityUtils.NO_AUTHORITIES);
                }
                return bean;
            }
        };
    }
}
