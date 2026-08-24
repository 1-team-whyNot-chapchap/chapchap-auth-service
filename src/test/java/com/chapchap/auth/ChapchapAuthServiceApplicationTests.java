package com.chapchap.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ChapchapAuthServiceApplicationTests {

    @Test
    void applicationDeclaresBootstrapAnnotations() {
        assertTrue(
            ChapchapAuthServiceApplication.class.isAnnotationPresent(SpringBootApplication.class)
        );
        assertTrue(
            ChapchapAuthServiceApplication.class.isAnnotationPresent(ConfigurationPropertiesScan.class)
        );
    }

}
