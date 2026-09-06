package com.chapchap.auth.global.security.servicejwt;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RsaServiceKeyMaterialTest {
    @Test
    void exposesTheConfiguredPublicKeyAsStandardJwks() throws Exception {
        InternalServiceJwtTestSupport.Fixture fixture = InternalServiceJwtTestSupport.fixture();

        JwksDocument.Jwk jwk = JwksDocument.from("auth-1", fixture.keys().publicKey()).keys().getFirst();

        assertThat(jwk.kty()).isEqualTo("RSA");
        assertThat(jwk.use()).isEqualTo("sig");
        assertThat(jwk.alg()).isEqualTo("RS256");
        assertThat(jwk.kid()).isEqualTo("auth-1");
        assertThat(unsigned(jwk.n())).isEqualTo(fixture.keys().publicKey().getModulus());
        assertThat(unsigned(jwk.e())).isEqualTo(fixture.keys().publicKey().getPublicExponent());
    }

    @Test
    void rejectsMissingOrMismatchedKeyMaterial() throws Exception {
        InternalServiceJwtTestSupport.Fixture first = InternalServiceJwtTestSupport.fixture();
        InternalServiceJwtTestSupport.Fixture second = InternalServiceJwtTestSupport.fixture();

        assertThatThrownBy(() -> RsaServiceKeyMaterial.fromPem("", first.properties().publicKeyPem()))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> RsaServiceKeyMaterial.fromPem(
                first.properties().privateKeyPem(), second.properties().publicKeyPem()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("do not match");
    }

    private static BigInteger unsigned(String value) {
        return new BigInteger(1, Base64.getUrlDecoder().decode(value));
    }
}
