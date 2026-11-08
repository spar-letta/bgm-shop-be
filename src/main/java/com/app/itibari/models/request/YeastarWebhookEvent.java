package com.app.itibari.models.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class YeastarWebhookEvent {

    private String event;

    @JsonProperty("call_id")
    private String callId;

    private String caller;

    private String callee;

    private String status;

    private String timestamp;

    @JsonProperty("call_direction")
    private String callDirection;

    // Populated by the PBX on failure events (e.g. NO_ROUTE, CONGESTION, BUSY)
    private String reason;
}