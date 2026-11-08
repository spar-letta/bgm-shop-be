package com.app.itibari;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;
/**
 * Created by christopher oigo (oigoc60@gmail.com) on May 2025
 *
 */

@EnableFeignClients
@SpringBootApplication
@EnableScheduling
public class ItibariApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC+03:00"))); // Set default time zone to East Africa Time (EAT)
        SpringApplication.run(ItibariApplication.class, args);


    }

    @Bean
    public Module springDataPageModule() {
        return new SimpleModule().addSerializer(Page.class, new JsonSerializer<>() {
            @Override
            public void serialize(Page value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeStartObject();
                gen.writeNumberField("totalElements", value.getTotalElements());
                gen.writeNumberField("pageSize", value.getNumberOfElements());
                gen.writeNumberField("totalPages", value.getTotalPages());
                gen.writeBooleanField("last", value.isLast());
                gen.writeBooleanField("first", value.isFirst());
                gen.writeNumberField("pageNumber", value.getNumber());
                gen.writeFieldName("content");
                serializers.defaultSerializeValue(value.getContent(), gen);
                gen.writeEndObject();
            }
        });
    }


    @Bean
    public Storage storage() {
        return StorageOptions.getDefaultInstance().getService();
    }



}
