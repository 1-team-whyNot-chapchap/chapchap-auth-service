package com.chapchap.auth.global.config.servicetoken;

import com.chapchap.auth.domain.servicetoken.dto.RsaServiceKeyMaterial;
import com.chapchap.auth.domain.servicetoken.response.JwksDocument;
import com.chapchap.auth.domain.servicetoken.service.ServiceClientRegistry;
import com.chapchap.auth.domain.servicetoken.service.ServiceTokenIssuer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "internal-service-jwt", name = "enabled", havingValue = "true")
public class InternalServiceJwtConfiguration {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Bean
    RsaServiceKeyMaterial internalServiceKeyMaterial(InternalServiceJwtProperties properties) {
        return RsaServiceKeyMaterial.fromPem(properties.privateKeyPem(), properties.publicKeyPem());
    }

    @Bean
    ServiceClientRegistry internalServiceClientRegistry(InternalServiceJwtProperties properties) {
        return new ServiceClientRegistry(properties.clients());
    }

    @Bean
    ServiceTokenIssuer internalServiceTokenIssuer(InternalServiceJwtProperties properties,
                                                   RsaServiceKeyMaterial keyMaterial,
                                                   ServiceClientRegistry clientRegistry) {
        return new ServiceTokenIssuer(properties, keyMaterial, clientRegistry,
                Clock.system(KST));
    }

    @Bean
    JwksDocument internalServiceJwks(InternalServiceJwtProperties properties,
                                     RsaServiceKeyMaterial keyMaterial) {
        return JwksDocument.from(properties.keyId(), keyMaterial.publicKey());
    }
}
