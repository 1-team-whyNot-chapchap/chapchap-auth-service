package com.chapchap.auth.global.security.servicejwt;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(prefix = "internal-service-jwt", name = "enabled", havingValue = "true")
public class ServiceTokenController {
    private final ServiceTokenIssuer tokenIssuer;

    public ServiceTokenController(ServiceTokenIssuer tokenIssuer) {
        this.tokenIssuer = tokenIssuer;
    }

    @PostMapping(value = "/internal/v1/service-tokens", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<ServiceTokenResponse> issue(
            @RequestParam(name = "grant_type", required = false) String grantType,
            @RequestParam(name = "client_id", required = false) String clientId,
            @RequestParam(name = "client_secret", required = false) String clientSecret,
            @RequestParam(required = false) String audience,
            @RequestParam(required = false) String scope) {
        return ResponseEntity.ok(tokenIssuer.issue(grantType, clientId, clientSecret, audience, scope));
    }
}
