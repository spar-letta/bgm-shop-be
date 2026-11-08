package com.app.itibari.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class YeastarConfiguration {

    @Value("${yeastar.pbx.url}")
    private String url;

    @Value("${yeastar.pbx.access-id}")
    private String accessId;

    @Value("${yeastar.pbx.access-key}")
    private String accessKey;

    @Value("${yeastar.pbx.token-refresh-buffer-seconds:300}")
    private int tokenRefreshBufferSeconds;

    @Value("${yeastar.pbx.default-extension}")
    private String defaultExtension;
}