package com.app.itibari.models.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class YeastarExtensionListResponse {

    private String status;

    @JsonProperty("extension_list")
    private List<YeastarExtension> extensionList;

    public boolean isSuccess() {
        return "Success".equalsIgnoreCase(status);
    }
}