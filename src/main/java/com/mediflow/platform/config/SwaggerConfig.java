package com.mediflow.platform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI mediflowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MediFlow Platform API")
                        .description("REST API documentation for MediFlow — a healthcare management platform")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("MediFlow Team")
                                .email("support@mediflow.com"))
                        .license(new License()
                                .name("Private — All rights reserved")));
    }

    @Bean
    public GroupedOpenApi patientApi() {
        return GroupedOpenApi.builder()
                .group("01 - Patient Management")
                .pathsToMatch("/api/v1/patients/**")
                .build();
    }

    @Bean
    public GroupedOpenApi appointmentApi() {
        return GroupedOpenApi.builder()
                .group("02 - Appointment Management")
                .pathsToMatch("/api/v1/appointments/**")
                .build();
    }

    @Bean
    public GroupedOpenApi doctorApi() {
        return GroupedOpenApi.builder()
                .group("03 - Doctor Management")
                .pathsToMatch("/api/v1/doctors/**")
                .build();
    }

    @Bean
    public GroupedOpenApi billingApi() {
        return GroupedOpenApi.builder()
                .group("04 - Billing & Invoicing")
                .pathsToMatch("/api/v1/billing/**", "/api/v1/invoices/**")
                .build();
    }

    @Bean
    public GroupedOpenApi allApis() {
        return GroupedOpenApi.builder()
                .group("00 - All APIs")
                .pathsToMatch("/api/**")
                .build();
    }
}
