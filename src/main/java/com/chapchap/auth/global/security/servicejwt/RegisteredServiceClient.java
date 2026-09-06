package com.chapchap.auth.global.security.servicejwt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

record RegisteredServiceClient(String clientId, byte[] secret, String subject,
                               Set<String> allowedAudiences, Set<String> allowedScopes) {
    boolean authenticates(String candidateSecret) {
        byte[] candidate = candidateSecret == null ? new byte[0] : candidateSecret.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(secret, candidate);
    }
}
