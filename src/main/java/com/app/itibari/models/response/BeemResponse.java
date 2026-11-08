package com.app.itibari.models.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BeemResponse {

    private boolean successful;

    @JsonProperty("request_id")
    private int requestId;

    private int code;

    private String message;

    private int valid;

    private int invalid;

    private int duplicates;
}
