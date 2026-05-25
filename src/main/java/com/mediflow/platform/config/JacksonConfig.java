package com.mediflow.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Central Jackson ObjectMapper bean for the MediFlow platform.
 *
 * Uses Jackson 3.x (tools.jackson) — bundled by Spring Boot 4.x.
 * findAndAddModules() discovers the Java time module on the classpath so that
 * LocalDate / LocalDateTime / LocalTime serialize as ISO strings, not arrays.
 * Per-field @JsonFormat annotations control the exact pattern on each DTO field.
 *
 * BigDecimal precision guarantees:
 * - WRITE_BIGDECIMAL_AS_PLAIN: prevents scientific notation (e.g., 1E+3 → 1000.00)
 * - USE_BIG_DECIMAL_FOR_FLOATS: JSON float tokens are parsed as BigDecimal,
 *   not double, eliminating IEEE 754 rounding at the deserialization boundary.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .findAndAddModules()
                .enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
                .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
                .build();
    }
}
