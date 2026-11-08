package com.app.itibari.models.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class YeastarApiResponse {

    @JsonProperty("call_id")
    private String callId;

    @JsonProperty("errmsg")
    private String message;

    @JsonProperty("errcode")
    private String errorCode;
}
