package com.chapchap.auth.global.security.servicejwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ServiceTokenControllerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        ServiceTokenIssuer issuer = InternalServiceJwtTestSupport.fixture().issuer();
        mockMvc = MockMvcBuilders.standaloneSetup(new ServiceTokenController(issuer))
                .setControllerAdvice(new InternalServiceJwtExceptionHandler())
                .build();
    }

    @Test
    void returnsOAuthStyleTokenResponse() throws Exception {
        mockMvc.perform(post("/internal/v1/service-tokens")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "client_credentials")
                        .param("client_id", "customer-service")
                        .param("client_secret", InternalServiceJwtTestSupport.CLIENT_SECRET)
                        .param("audience", "chapchap-customer-ai")
                        .param("scope", "customer-ai.invoke"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(300))
                .andExpect(jsonPath("$.scope").value("customer-ai.invoke"));
    }

    @Test
    void mapsInvalidClientAndScopeToStableErrors() throws Exception {
        mockMvc.perform(post("/internal/v1/service-tokens")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "client_credentials")
                        .param("client_id", "customer-service")
                        .param("client_secret", "wrong")
                        .param("audience", "chapchap-customer-ai")
                        .param("scope", "customer-ai.invoke"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_client"));

        mockMvc.perform(post("/internal/v1/service-tokens")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "client_credentials")
                        .param("client_id", "customer-service")
                        .param("client_secret", InternalServiceJwtTestSupport.CLIENT_SECRET)
                        .param("audience", "chapchap-customer-ai")
                        .param("scope", "customer-ai.write"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("insufficient_scope"));
    }
}
