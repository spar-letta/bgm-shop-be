package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DuplicateReceiptResponse {

    private String message;

    private boolean isDuplicate;

    public DuplicateReceiptResponse(String message, boolean isDuplicate) {

        this.message = message;
        this.isDuplicate = isDuplicate;
    }
}
