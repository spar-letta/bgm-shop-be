package com.app.itibari.utils.exceptions.rest;

public class UnauthorizedRestApiException extends RestApiException {

    private static final long serialVersionUID = 1L;

    public UnauthorizedRestApiException() {

        super(RestApiHttpStatus.UNAUTHORIZED);
    }
}
