package com.f4sitive.account.config;

import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.reactive.HttpComponentsClientHttpConnector;

@Configuration(proxyBeanMethods = false)
class ClientConfig {
    @Bean
    RestTemplateCustomizer restTemplateCustomizer(CloseableHttpClient httpClient) {
        return restTemplate -> restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
    }

    @Bean
    WebClientCustomizer webClientCustomizer(CloseableHttpAsyncClient reactiveHttpClient) {
        return webClientBuilder -> webClientBuilder.clientConnector(new HttpComponentsClientHttpConnector(reactiveHttpClient));
    }
}
