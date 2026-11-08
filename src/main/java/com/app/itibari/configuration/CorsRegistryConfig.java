package com.app.itibari.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsRegistryConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry
                .addMapping("/**")
                .allowedOrigins(
          "http://localhost:8000",
                        "http://localhost:8080",
                        "http://localhost:3000",
                        "https://frontend-181718142186.europe-west1.run.app/",
                        "https://dev.itibari.io/",
                        "https://frontend-1088261418762.europe-west1.run.app/",
                        "https://system.itibari.io/",
                        "https://ims.itibari.io/")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
