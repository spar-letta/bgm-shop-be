package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class FileResponseDTO {

    private String url;

    private String contentType;

    private String description;

    private UUID publicId;

    private boolean active;

    public FileResponseDTO(String url, String contentType, String description, UUID publicId, boolean active) {

        this.url = url;
        this.contentType = contentType;
        this.description = description;
        this.publicId = publicId;
        this.active = active;
    }
}
