package com.f4sitive.account.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.Optional;

@Controller
@RequestMapping("/login")
public class LoginController {
    private final RequestCache requestCache;
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    public LoginController(RequestCache requestCache) {
        this.requestCache = requestCache;
    }

    @GetMapping("/oauth2/success/{registrationId}")
    public String get_oauth2_success(Principal principal, @PathVariable("registrationId") String registrationId, HttpServletRequest request, HttpServletResponse response) {
        return "login/oauth2/success";
    }

    @PostMapping("/oauth2/success/{registrationId}")
    public void post_oauth2_success(Principal principal, @PathVariable("registrationId") String registrationId, HttpServletRequest request, HttpServletResponse response) {
        Optional.ofNullable(requestCache.getRequest(request, response))
                .map(SavedRequest::getRedirectUrl)
                .ifPresent(redirectUrl -> {
                    try {
                        redirectStrategy.sendRedirect(request, response, redirectUrl);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
