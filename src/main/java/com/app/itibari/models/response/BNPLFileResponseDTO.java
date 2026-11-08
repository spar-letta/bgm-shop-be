package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class BNPLFileResponseDTO {
    private String url;

    private String contentType;

    private String description;

    private String documentName;

    private String documentType;

    private LocalDate expiryDate;

    private boolean expired;

    private boolean rejected;

    private String rejectionReason;
}
