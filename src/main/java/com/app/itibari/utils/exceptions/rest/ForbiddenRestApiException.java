package com.app.itibari.utils.exceptions.rest;

public class ForbiddenRestApiException extends RestApiException {

    private static final long serialVersionUID = 1L;

    public ForbiddenRestApiException() {

        super(RestApiHttpStatus.FORBIDDEN);
    }
}
