package com.app.itibari.configuration;

import org.springframework.beans.factory.annotation.Value;
import com.africastalking.AfricasTalking;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

/**
 * Created by christopher oigo (oigoc60@gmail.com) on May 2025
 */

//  updates
@Configuration
public class AfrikaStalkingConfig {
    @Value("${africastalking.username}")
    private String username;

    @Value("${africastalking.apiKey}")
    private String apiKey;

    @PostConstruct
    public void init() {
        AfricasTalking.initialize(username, apiKey);
    }
}
