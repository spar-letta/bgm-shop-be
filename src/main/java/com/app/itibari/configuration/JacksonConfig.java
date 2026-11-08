package com.app.itibari.configuration;

import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    // Frontend sends "" for unset list fields (e.g. TumaSmsDto.customVariable); read them as empty collections
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer emptyStringAsEmptyCollection() {
        return builder -> builder.postConfigurer(mapper -> mapper.coercionConfigFor(LogicalType.Collection)
                .setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsEmpty));
    }
}
