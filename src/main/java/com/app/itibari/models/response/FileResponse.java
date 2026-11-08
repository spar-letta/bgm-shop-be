package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Getter
@Setter
public class FileResponse {

    private String content;

    private String contentType;

    private String url;

    public FileResponse(byte[] file, String contentType) {

        byte[] encodeBase64 = Base64.getEncoder().encode(file);
        content = new String(encodeBase64, StandardCharsets.UTF_8);
        this.contentType = contentType;
    }
}
