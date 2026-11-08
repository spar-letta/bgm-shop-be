package com.app.itibari.models.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class YeastarOutboundCallRequest {

    @JsonProperty("caller")
    private String extid;

    @JsonProperty("callee")
    private String outto;

//    @JsonProperty("dial_permission")
//    private String dial_permission;

    @JsonProperty("auto_answer")
    private String autoanswer = "YES";

}
