package com.app.itibari.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsRegistryConfig implements WebMvcConfigurer {

    @Value("${CORS_ALLOWED_ORIGINS:}")
    private String extraAllowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = new ArrayList<>(Arrays.asList(
                "http://localhost:8000",
                "http://localhost:8080",
                "http://localhost:3000",
                "http://35.190.215.242",
                "https://frontend-181718142186.europe-west1.run.app",
                "https://dev.itibari.io",
                "https://frontend-1088261418762.europe-west1.run.app",
                "https://system.itibari.io",
                "https://ims.itibari.io"
        ));
        if (StringUtils.hasText(extraAllowedOrigins)) {
            Arrays.stream(extraAllowedOrigins.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .forEach(origins::add);
        }

        registry
                .addMapping("/**")
                .allowedOrigins(origins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
