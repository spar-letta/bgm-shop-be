package com.app.itibari.models.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class BeemRequest {

    @JsonProperty("source_addr")
    private String sourceAddr;

    @JsonProperty("schedule_time")
    private String scheduleTime;

    private int encoding;

    private String message;

    private List<Receipient> recipients;
}
