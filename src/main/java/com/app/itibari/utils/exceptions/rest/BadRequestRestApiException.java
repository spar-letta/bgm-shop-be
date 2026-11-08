package com.app.itibari.utils.exceptions.rest;

import org.springframework.stereotype.Component;

@Component
public class BadRequestRestApiException extends RestApiException {

    private static final long serialVersionUID = 1L;

    public BadRequestRestApiException() {

        super(RestApiHttpStatus.BAD_REQUEST);
    }
}
