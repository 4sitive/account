package com.f4sitive.account.controller.internal;

import com.f4sitive.account.service.AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class AuthorizationController {
    private final AuthorizationService authorizationService;

    public AuthorizationController(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @GetMapping("/internal/authorization")
    public List<OAuth2Authorization> authorization(@RequestHeader("User-Id") String userId, @RequestHeader(value = "Device-Id", required = false) String deviceId) {
        return authorizationService.findAllByUserId(userId)
                .stream()
                .filter(oauth2Authorization -> !StringUtils.hasText(deviceId) || deviceId.equals(oauth2Authorization.getId()))
                .collect(Collectors.toList());
    }
}
