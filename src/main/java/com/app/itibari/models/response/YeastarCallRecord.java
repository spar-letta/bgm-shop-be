package com.app.itibari.models.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class YeastarCallRecord {

    @JsonProperty("call_id")
    private String callId;

    private String caller;

    private String callee;

    @JsonProperty("start_time")
    private String startTime;

    @JsonProperty("end_time")
    private String endTime;

    private int duration;

    @JsonProperty("talk_duration")
    private int talkDuration;

    private String status;

    private String direction;

    @JsonProperty("recording_file")
    private String recordingFile;
}