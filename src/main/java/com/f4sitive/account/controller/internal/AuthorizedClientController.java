package com.f4sitive.account.controller.internal;

import com.f4sitive.account.service.AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class AuthorizedClientController {
    private final AuthorizedClientService authorizedClientService;

    public AuthorizedClientController(AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
    }

    @GetMapping("/internal/authorizedClient")
    public List<OAuth2AuthorizedClient> authorizedClient(@RequestHeader("User-Id") String userId) {
        return authorizedClientService.findAllByUserId(userId);
    }

    @GetMapping("/internal/authorizedClient/{clientRegistrationId}")
    public OAuth2AuthorizedClient authorizedClient(@RequestHeader("User-Id") String userId, @PathVariable String clientRegistrationId, @RequestParam Map<String, Object> attributes) {
        return authorizedClientService.authorize(clientRegistrationId, userId, attributes);
    }
}
