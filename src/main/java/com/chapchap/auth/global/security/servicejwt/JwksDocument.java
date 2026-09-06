package com.chapchap.auth.global.security.servicejwt;

import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public record JwksDocument(List<Jwk> keys) {
    public static JwksDocument from(String keyId, RSAPublicKey publicKey) {
        if (keyId == null || keyId.isBlank()) {
            throw new IllegalStateException("Internal service JWT key ID must not be blank.");
        }
        return new JwksDocument(List.of(new Jwk("RSA", "sig", "RS256", keyId.trim(),
                encode(publicKey.getModulus()), encode(publicKey.getPublicExponent()))));
    }

    private static String encode(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record Jwk(String kty, String use, String alg, String kid, String n, String e) {
    }
}
