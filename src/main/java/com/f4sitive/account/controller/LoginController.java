package com.f4sitive.account.controller;

import lombok.SneakyThrows;
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

    @SneakyThrows
    @GetMapping("/oauth2/success/{provider}")
    public void get_oauth2_success(Principal principal, @PathVariable("provider") String provider, HttpServletRequest request, HttpServletResponse response) {
        String redirectUrl= Optional.ofNullable(requestCache.getRequest(request, response))
                .map(SavedRequest::getRedirectUrl)
                .orElse("javascript://history.go(-1)");
        redirectStrategy.sendRedirect(request, response, redirectUrl);
//        return "login/oauth2/success";
    }

    @PostMapping("/oauth2/success/{provider}")
    public void post_oauth2_success(Principal principal, @PathVariable("provider") String provider, HttpServletRequest request, HttpServletResponse response) {
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
