package com.chapchap.auth.global.security.servicejwt;

import io.jsonwebtoken.Jwts;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ServiceTokenIssuer {
    private static final long MAXIMUM_TTL_SECONDS = 300;
    private final String issuer;
    private final String keyId;
    private final long tokenTtlSeconds;
    private final RsaServiceKeyMaterial keyMaterial;
    private final ServiceClientRegistry clientRegistry;
    private final Clock clock;

    public ServiceTokenIssuer(InternalServiceJwtProperties properties, RsaServiceKeyMaterial keyMaterial,
                              ServiceClientRegistry clientRegistry, Clock clock) {
        if (properties.issuer() == null || properties.issuer().isBlank()
                || properties.keyId() == null || properties.keyId().isBlank()
                || properties.tokenTtlSeconds() < 1 || properties.tokenTtlSeconds() > MAXIMUM_TTL_SECONDS) {
            throw new IllegalStateException("Internal service JWT issuer, key ID, or TTL is invalid.");
        }
        this.issuer = properties.issuer().trim();
        this.keyId = properties.keyId().trim();
        this.tokenTtlSeconds = properties.tokenTtlSeconds();
        this.keyMaterial = keyMaterial;
        this.clientRegistry = clientRegistry;
        this.clock = clock;
    }

    public ServiceTokenResponse issue(String grantType, String clientId, String clientSecret,
                                      String audience, String requestedScope) {
        if (!"client_credentials".equals(grantType) || audience == null || audience.isBlank()
                || requestedScope == null || requestedScope.isBlank()) {
            throw InternalServiceJwtException.invalidRequest();
        }
        RegisteredServiceClient client = clientRegistry.authenticate(clientId, clientSecret);
        String normalizedAudience = audience.trim();
        Set<String> scopes = normalizeScopes(requestedScope);
        if (!client.allowedAudiences().contains(normalizedAudience) || !client.allowedScopes().containsAll(scopes)) {
            throw InternalServiceJwtException.insufficientScope();
        }
        Instant issuedAt = clock.instant();
        String normalizedScope = String.join(" ", scopes);
        String token = Jwts.builder()
                .header().keyId(keyId).type("JWT").and()
                .issuer(issuer)
                .subject(client.subject())
                .audience().add(normalizedAudience).and()
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusSeconds(tokenTtlSeconds)))
                .id(UUID.randomUUID().toString())
                .claim("scope", normalizedScope)
                .signWith(keyMaterial.privateKey(), Jwts.SIG.RS256)
                .compact();
        return new ServiceTokenResponse(token, "Bearer", tokenTtlSeconds, normalizedScope);
    }

    private static Set<String> normalizeScopes(String requestedScope) {
        Set<String> scopes = Arrays.stream(requestedScope.trim().split("\\s+"))
                .filter(value -> !value.isBlank()).collect(Collectors.toCollection(TreeSet::new));
        if (scopes.isEmpty()) {
            throw InternalServiceJwtException.invalidRequest();
        }
        return scopes;
    }
}
