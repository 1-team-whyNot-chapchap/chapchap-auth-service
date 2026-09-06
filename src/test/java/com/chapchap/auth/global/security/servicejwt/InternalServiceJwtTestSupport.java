package com.chapchap.auth.global.security.servicejwt;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

final class InternalServiceJwtTestSupport {
    static final String CLIENT_SECRET = "customer-service-secret-at-least-32-characters";
    static final Instant NOW = Instant.parse("2026-09-07T01:00:00Z");

    private InternalServiceJwtTestSupport() {
    }

    static Fixture fixture() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        InternalServiceJwtProperties properties = new InternalServiceJwtProperties(
                true,
                "chapchap-auth-service",
                "auth-1",
                300,
                pem("PRIVATE KEY", pair.getPrivate().getEncoded()),
                pem("PUBLIC KEY", pair.getPublic().getEncoded()),
                Map.of("customer-service", new InternalServiceJwtProperties.Client(
                        CLIENT_SECRET,
                        "customer-service",
                        Set.of("chapchap-customer-ai"),
                        Set.of("customer-ai.invoke", "customer-ai.read")
                ))
        );
        RsaServiceKeyMaterial keyMaterial = RsaServiceKeyMaterial.fromPem(
                properties.privateKeyPem(), properties.publicKeyPem());
        ServiceTokenIssuer issuer = new ServiceTokenIssuer(
                properties,
                keyMaterial,
                new ServiceClientRegistry(properties.clients()),
                Clock.fixed(NOW, ZoneId.of("Asia/Seoul"))
        );
        return new Fixture(properties, keyMaterial, issuer);
    }

    private static String pem(String type, byte[] encoded) {
        return "-----BEGIN " + type + "-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(encoded)
                + "\n-----END " + type + "-----";
    }

    record Fixture(InternalServiceJwtProperties properties, RsaServiceKeyMaterial keys,
                   ServiceTokenIssuer issuer) {
    }
}
