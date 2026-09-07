package com.chapchap.auth.global.config.json;

import com.chapchap.auth.domain.auth.client.portone.PortOneIdentityVerificationClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class RestClientRuntimeConfigurationTest {
    @Test
    void wiresIdentityClientWithBootRestClientBuilderWithoutMakingNetworkCalls() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
                .withUserConfiguration(PortOneIdentityVerificationClient.class)
                .withPropertyValues("PORTONE_API_SECRET=local-test-not-a-real-secret")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(RestClient.Builder.class);
                    assertThat(context).hasSingleBean(PortOneIdentityVerificationClient.class);
                });
    }
}
