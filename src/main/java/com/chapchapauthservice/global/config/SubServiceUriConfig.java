package com.chapchapauthservice.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sub-service")
public record SubServiceUriConfig(
    String frontendCallbackUri
) {

}
