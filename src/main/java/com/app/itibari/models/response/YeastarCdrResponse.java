package com.app.itibari.models.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class YeastarCdrResponse {

    private String status;

    @JsonProperty("total_number")
    private int totalNumber;

    @JsonProperty("cdr_list")
    private List<YeastarCallRecord> cdrList;

    public boolean isSuccess() {
        return "Success".equalsIgnoreCase(status);
    }
}