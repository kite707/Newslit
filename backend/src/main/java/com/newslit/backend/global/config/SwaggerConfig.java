package com.newslit.backend.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        // Security Scheme 정의
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");

        // Security Requirement 정의
        SecurityRequirement securityRequirement = new SecurityRequirement().addList("BearerAuth");
        return new OpenAPI()
                .components(new Components())
                .addSecurityItem(securityRequirement)
                .schemaRequirement("BearerAuth", securityScheme)
                .info(apiInfo());
    }

    private Info apiInfo() {
        return new Info()
                .title("Newslit Springdoc")
                .description("Springdoc을 사용한 Newslit Swagger UI")
                .version("1.0.0");
    }
}