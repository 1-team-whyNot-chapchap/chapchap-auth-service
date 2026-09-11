package com.chapchap.auth.global.config.servicetoken;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.Set;

@ConfigurationProperties(prefix = "internal-service-jwt")
public record InternalServiceJwtProperties(
        boolean enabled,
        String issuer,
        String keyId,
        long tokenTtlSeconds,
        String privateKeyPem,
        String publicKeyPem,
        Map<String, Client> clients
) {
    public record Client(String secret, String subject, Set<String> allowedAudiences, Set<String> allowedScopes) {
    }
}
