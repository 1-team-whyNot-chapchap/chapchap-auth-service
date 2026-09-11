package com.chapchap.auth.domain.servicetoken.dto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

public record RegisteredServiceClient(String clientId, byte[] secret, String subject,
                               Set<String> allowedAudiences, Set<String> allowedScopes) {
    public boolean authenticates(String candidateSecret) {
        byte[] candidate = candidateSecret == null ? new byte[0] : candidateSecret.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(secret, candidate);
    }
}
