package com.app.itibari.models.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class YeastarCdrRequest {

    @JsonProperty("caller")
    private String caller;

    @JsonProperty("callee")
    private String callee;

    @JsonProperty("start_time")
    private String startTime;

    @JsonProperty("end_time")
    private String endTime;

    @JsonProperty("page")
    @Builder.Default
    private int page = 1;

    @JsonProperty("limit")
    @Builder.Default
    private int limit = 20;
}