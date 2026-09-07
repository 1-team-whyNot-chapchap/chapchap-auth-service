package com.chapchap.auth.global.config.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Supplies the Jackson 2 mapper used by existing filters and Kafka contracts. */
@Configuration(proxyBeanMethods = false)
public class LegacyJsonConfiguration {
    @Bean
    public ObjectMapper legacyObjectMapper() {
        return new ObjectMapper();
    }
}
