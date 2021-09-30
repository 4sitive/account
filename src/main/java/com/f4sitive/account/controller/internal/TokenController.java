package com.f4sitive.account.controller.internal;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TokenController {
    private final Cache cache;

    public TokenController(CacheManager cacheManager) {
        this.cache = cacheManager.getCache("TOKEN");
    }

    @GetMapping("/internal/token/{key}")
    public JsonNode token(@PathVariable("key") String key){
        return cache.get(key, JsonNode.class);
    }
}
