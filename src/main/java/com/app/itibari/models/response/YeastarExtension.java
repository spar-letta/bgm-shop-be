package com.app.itibari.models.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class YeastarExtension {

    private String number;

    private String name;

    private String status;

    @JsonProperty("caller_id_name")
    private String callerIdName;

    private String email;

    private String mobile;
}