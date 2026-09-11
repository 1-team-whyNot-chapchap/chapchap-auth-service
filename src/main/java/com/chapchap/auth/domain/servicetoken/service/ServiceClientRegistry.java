package com.chapchap.auth.domain.servicetoken.service;

import com.chapchap.auth.global.config.servicetoken.InternalServiceJwtProperties;
import com.chapchap.auth.domain.servicetoken.dto.RegisteredServiceClient;
import com.chapchap.auth.global.exception.servicetoken.InternalServiceJwtException;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class ServiceClientRegistry {
    private static final int MINIMUM_SECRET_LENGTH = 32;
    private final Map<String, RegisteredServiceClient> clients;

    public ServiceClientRegistry(Map<String, InternalServiceJwtProperties.Client> configuredClients) {
        if (configuredClients == null || configuredClients.isEmpty()) {
            throw new IllegalStateException("At least one internal service client must be configured.");
        }
        Map<String, RegisteredServiceClient> validated = new LinkedHashMap<>();
        configuredClients.forEach((clientId, client) -> validated.put(
                requireText(clientId, "client ID"), validate(clientId, client)));
        this.clients = Map.copyOf(validated);
    }

    RegisteredServiceClient authenticate(String clientId, String clientSecret) {
        RegisteredServiceClient client = clientId == null ? null : clients.get(clientId.trim());
        if (client == null || !client.authenticates(clientSecret)) {
            throw InternalServiceJwtException.invalidClient();
        }
        return client;
    }

    private RegisteredServiceClient validate(String clientId, InternalServiceJwtProperties.Client client) {
        if (client == null || client.secret() == null || client.secret().length() < MINIMUM_SECRET_LENGTH) {
            throw new IllegalStateException("Internal service client secrets must contain at least 32 characters.");
        }
        return new RegisteredServiceClient(clientId, client.secret().getBytes(StandardCharsets.UTF_8),
                requireText(client.subject(), "client subject"),
                requireTextSet(client.allowedAudiences(), "allowed audiences"),
                requireTextSet(client.allowedScopes(), "allowed scopes"));
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Internal service " + field + " must not be blank.");
        }
        return value.trim();
    }

    private static Set<String> requireTextSet(Set<String> values, String field) {
        if (values == null || values.isEmpty() || values.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalStateException("Internal service " + field + " must not be empty.");
        }
        return values.stream().map(String::trim).collect(Collectors.toUnmodifiableSet());
    }
}
