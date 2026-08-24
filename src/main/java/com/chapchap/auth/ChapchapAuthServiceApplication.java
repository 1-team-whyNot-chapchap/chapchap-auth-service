package com.chapchap.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ChapchapAuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChapchapAuthServiceApplication.class, args);
    }

}
