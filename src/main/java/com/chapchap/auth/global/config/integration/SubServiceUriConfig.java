package com.chapchap.auth.global.config.integration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sub-service")
public record SubServiceUriConfig(
    String frontendCallbackUri
) {

}
