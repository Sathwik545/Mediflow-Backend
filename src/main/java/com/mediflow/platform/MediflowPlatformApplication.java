package com.mediflow.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * MediFlow Platform — Spring Boot entry point.
 *
 * @EnableJpaAuditing activates Spring Data JPA's auditing engine.
 * auditorAwareRef wires it to SecurityAuditorAware, which extracts
 * the authenticated user's email from the JWT SecurityContext.
 * This populates createdBy/updatedBy on every entity that extends
 * BaseAuditEntity — fully automatically, with no service-layer code.
 */
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "securityAuditorAware")
public class MediflowPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(MediflowPlatformApplication.class, args);
	}

}
