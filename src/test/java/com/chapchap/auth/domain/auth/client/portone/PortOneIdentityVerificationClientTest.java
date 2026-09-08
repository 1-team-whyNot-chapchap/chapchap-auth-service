package com.chapchap.auth.domain.auth.client.portone;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PortOneIdentityVerificationClientTest {

    @Test
    void configuresPortOneAuthorizationSchemeWithRequiredWhitespace() {
        RestClient.Builder builder = mock(RestClient.Builder.class);
        RestClient restClient = mock(RestClient.class);
        when(builder.baseUrl("https://api.portone.io")).thenReturn(builder);
        when(builder.defaultHeader(HttpHeaders.AUTHORIZATION, "PortOne api-secret")).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);

        new PortOneIdentityVerificationClient(builder, "api-secret");

        verify(builder).defaultHeader(HttpHeaders.AUTHORIZATION, "PortOne api-secret");
    }

    @Test
    void rejectsBlankApiSecretBeforeCreatingClient() {
        RestClient.Builder builder = mock(RestClient.Builder.class);

        assertThrows(IllegalStateException.class,
                () -> new PortOneIdentityVerificationClient(builder, " "));
    }
}
