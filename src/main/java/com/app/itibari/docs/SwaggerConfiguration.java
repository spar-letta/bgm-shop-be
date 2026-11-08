package com.app.itibari.docs;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                contact = @Contact(
                        name = "Christopher Oigo",
                        email = "itibati.co.ta"
                )
        )
)
public class SwaggerConfiguration {

    /**
     * HTTP Bearer JWT: shows Authorize in Swagger UI and attaches Authorization to requests.
     * Without {@link OpenAPI#addSecurityItem}, operations do not inherit the scheme and Swagger
     * sends no header even after Authorize (clients see 401).
     * <p>
     * Public endpoints opt out via {@code @SecurityRequirements} on their controller/method.
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the raw token only (no quotes, no 'Bearer ' prefix).")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
