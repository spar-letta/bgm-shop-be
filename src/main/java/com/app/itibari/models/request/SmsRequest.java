package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SmsRequest {

    private String from;

    private String to;

    private String text;

    private String reference;
}
