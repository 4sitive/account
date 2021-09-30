package com.f4sitive.account.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public class TestController {
    @PostMapping("/test/status/{status}")
    public ResponseEntity<Map> test(@PathVariable int status, @RequestBody String body) {
        return ResponseEntity.status(status).body(Collections.singletonMap("status", HttpStatus.resolve(status).value()));
    }

    @PutMapping("/test/async/{status}")
    public CompletableFuture<Map> testAsync(@PathVariable int status, @RequestBody String body) {
        return CompletableFuture.supplyAsync(() -> Collections.singletonMap("status", HttpStatus.resolve(status).value()));
    }
}
