package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageDetail {

    private String to;

    private StatusDetail status;

    private int smsCount;
}
