package com.mediflow.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Central Jackson ObjectMapper bean for the MediFlow platform.
 *
 * Uses Jackson 3.x (tools.jackson) — bundled by Spring Boot 4.x.
 * findAndAddModules() discovers the Java time module on the classpath so that
 * LocalDate / LocalDateTime / LocalTime serialize as ISO strings, not arrays.
 * Per-field @JsonFormat annotations control the exact pattern on each DTO field.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .findAndAddModules()
                .build();
    }
}
