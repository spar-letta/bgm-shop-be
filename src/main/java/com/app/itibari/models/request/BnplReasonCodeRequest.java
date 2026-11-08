package com.app.itibari.models.request;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BnplReasonCodeRequest {

    private String code;
    private String description;
    private Boolean isActive;
}