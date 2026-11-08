package com.app.itibari.utils.exceptions.rest;

public class ConstraintValidationException extends RestApiException {

    private static final long serialVersionUID = 1L;

    public ConstraintValidationException() {

        super(RestApiHttpStatus.INTERNAL_SERVER_ERROR);
    }
}
