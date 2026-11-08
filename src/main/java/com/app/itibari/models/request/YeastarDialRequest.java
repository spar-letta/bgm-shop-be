package com.app.itibari.models.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class YeastarDialRequest {

    @JsonProperty("callee")
    private String callee;

    @JsonProperty("autoanswer")
    private String autoanswer = "yes";

    private Boolean isChurned = Boolean.FALSE;

    private Boolean buyNowPayLater = Boolean.FALSE;

    private Boolean locationChange = Boolean.FALSE;

    private Boolean orderAmendment = Boolean.FALSE;
}
