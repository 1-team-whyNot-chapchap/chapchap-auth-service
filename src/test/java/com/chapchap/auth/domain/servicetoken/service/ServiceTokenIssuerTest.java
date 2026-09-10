package com.chapchap.auth.domain.servicetoken.service;

import com.chapchap.auth.domain.servicetoken.support.InternalServiceJwtTestSupport;

import com.chapchap.auth.global.config.servicetoken.InternalServiceJwtProperties;
import com.chapchap.auth.global.exception.servicetoken.InternalServiceJwtException;
import com.chapchap.auth.domain.servicetoken.response.ServiceTokenResponse;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceTokenIssuerTest {
    @Test
    void issuesRs256TokenMatchingCustomerAiContract() throws Exception {
        InternalServiceJwtTestSupport.Fixture fixture = InternalServiceJwtTestSupport.fixture();

        ServiceTokenResponse response = fixture.issuer().issue(
                "client_credentials",
                "customer-service",
                InternalServiceJwtTestSupport.CLIENT_SECRET,
                "chapchap-customer-ai",
                "customer-ai.read  customer-ai.invoke customer-ai.invoke"
        );

        Jws<Claims> parsed = Jwts.parser()
                .verifyWith(fixture.keys().publicKey())
                .requireIssuer("chapchap-auth-service")
                .clock(() -> Date.from(InternalServiceJwtTestSupport.NOW))
                .build()
                .parseSignedClaims(response.accessToken());
        Claims claims = parsed.getPayload();

        assertThat(parsed.getHeader().getKeyId()).isEqualTo("auth-1");
        assertThat(parsed.getHeader().getAlgorithm()).isEqualTo("RS256");
        assertThat(claims.getSubject()).isEqualTo("customer-service");
        assertThat(claims.getAudience()).containsExactly("chapchap-customer-ai");
        assertThat(claims.get("scope", String.class)).isEqualTo("customer-ai.invoke customer-ai.read");
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getExpiration().toInstant().getEpochSecond()
                - claims.getIssuedAt().toInstant().getEpochSecond()).isEqualTo(300);
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(300);
    }

    @Test
    void rejectsBadGrantClientAudienceAndScopeWithoutLeakingCredentials() throws Exception {
        ServiceTokenIssuer issuer = InternalServiceJwtTestSupport.fixture().issuer();

        assertReason(issuer, "password", InternalServiceJwtTestSupport.CLIENT_SECRET,
                "chapchap-customer-ai", "customer-ai.invoke",
                InternalServiceJwtException.Reason.INVALID_REQUEST);
        assertReason(issuer, "client_credentials", "wrong-secret",
                "chapchap-customer-ai", "customer-ai.invoke",
                InternalServiceJwtException.Reason.INVALID_CLIENT);
        assertReason(issuer, "client_credentials", InternalServiceJwtTestSupport.CLIENT_SECRET,
                "other-service", "customer-ai.invoke",
                InternalServiceJwtException.Reason.INSUFFICIENT_SCOPE);
        assertReason(issuer, "client_credentials", InternalServiceJwtTestSupport.CLIENT_SECRET,
                "chapchap-customer-ai", "customer-ai.write",
                InternalServiceJwtException.Reason.INSUFFICIENT_SCOPE);
    }

    @Test
    void refusesTtlLongerThanCustomerAiMaximum() throws Exception {
        InternalServiceJwtTestSupport.Fixture fixture = InternalServiceJwtTestSupport.fixture();
        InternalServiceJwtProperties invalid = new InternalServiceJwtProperties(
                true, "chapchap-auth-service", "auth-1", 301,
                fixture.properties().privateKeyPem(), fixture.properties().publicKeyPem(),
                fixture.properties().clients());

        assertThatThrownBy(() -> new ServiceTokenIssuer(
                invalid,
                fixture.keys(),
                new ServiceClientRegistry(invalid.clients()),
                Clock.fixed(InternalServiceJwtTestSupport.NOW, ZoneId.of("Asia/Seoul"))))
                .isInstanceOf(IllegalStateException.class);
    }

    private static void assertReason(ServiceTokenIssuer issuer, String grantType, String secret,
                                     String audience, String scope,
                                     InternalServiceJwtException.Reason expected) {
        assertThatThrownBy(() -> issuer.issue(grantType, "customer-service", secret, audience, scope))
                .isInstanceOfSatisfying(InternalServiceJwtException.class,
                        exception -> {
                            assertThat(exception.reason()).isEqualTo(expected);
                            assertThat(exception.getMessage()).doesNotContain(secret);
                        });
    }
}
