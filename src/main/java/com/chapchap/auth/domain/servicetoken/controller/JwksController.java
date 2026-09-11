package com.chapchap.auth.domain.servicetoken.controller;

import com.chapchap.auth.domain.servicetoken.response.JwksDocument;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(prefix = "internal-service-jwt", name = "enabled", havingValue = "true")
public class JwksController {
    private final JwksDocument document;

    public JwksController(JwksDocument document) {
        this.document = document;
    }

    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<JwksDocument> keys() {
        return ResponseEntity.ok(document);
    }
}
