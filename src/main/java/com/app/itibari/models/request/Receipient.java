package com.app.itibari.models.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Receipient {

    @JsonProperty("recipient_id")
    private int recipientId;

    @JsonProperty("dest_addr")
    private String destAddr;

    public Receipient(int recipientId, String destAddr) {

        this.recipientId = recipientId;
        this.destAddr = destAddr;
    }
}
