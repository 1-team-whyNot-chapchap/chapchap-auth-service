package com.chapchap.auth;

import com.chapchap.auth.global.kafka.config.KafkaTopicProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.FileSystemResource;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Properties;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationConfigurationTest {
    @Test
    void kafkaTopicsBindToTheSubscriptionAndDeliveryTeamContract() throws Exception {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        new YamlPropertySourceLoader().load("main",
                new FileSystemResource("src/main/resources/application.yaml"))
                .forEach(environment.getPropertySources()::addLast);

        KafkaTopicProperties topics = Binder.get(environment)
                .bind("app.kafka.topics", KafkaTopicProperties.class).orElseThrow(IllegalStateException::new);

        assertThat(topics).isEqualTo(new KafkaTopicProperties(
                "msa4-team1.auth.user-events.v1", "msa4-team1.auth.user-events.v1.DLT",
                "msa4-team1.subscription.address-events.v1", "msa4-team1.subscription.address-events.v1.DLT",
                "msa4-team1.subscription.subscription-events.v1", "msa4-team1.subscription.subscription-events.v1.DLT"
        ));
    }

    @Test
    void exampleResolvesAllSettingsAndKeepsDatabaseInitializationExplicit() throws Exception {
        String yaml = Files.readString(Path.of("src/main/resources/application.yaml"));
        Properties example = new Properties();
        example.load(new StringReader(Files.readString(Path.of(".env.example"))));
        Pattern.compile("\\$\\{([A-Z][A-Z0-9_]*)(?=[:}])").matcher(yaml).results()
                .forEach(match -> assertThat(example).containsKey(match.group(1)));
        assertThat(example).containsKeys("IDENTITY_HMAC_SECRET", "PORTONE_API_SECRET");
        assertThat(Base64.getDecoder().decode(example.getProperty("JWT_SECRET")).length)
                .isGreaterThanOrEqualTo(32);
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        env.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        env.getPropertySources().addFirst(new PropertiesPropertySource("example", example));
        var loaded = new YamlPropertySourceLoader().load("application",
                new FileSystemResource("src/main/resources/application.yaml"));
        loaded.forEach(env.getPropertySources()::addLast);
        assertThat(env.getProperty("server.port")).isEqualTo("8081");
        assertThat(env.getProperty("spring.datasource.url")).isEqualTo("jdbc:mysql://localhost:3306/auth_db");
        assertThat(env.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(env.getProperty("spring.sql.init.mode")).isEqualTo("never");
        assertThat(env.getProperty("spring.web.error.include-stacktrace")).isEqualTo("never");
        assertThat(env.getProperty("internal-service-jwt.enabled")).isEqualTo("false");
        loaded.forEach(source -> {
            for (String key : ((org.springframework.core.env.EnumerablePropertySource<?>) source).getPropertyNames()) {
                assertThat(env.getProperty(key)).doesNotContain("${");
            }
        });
    }
}
