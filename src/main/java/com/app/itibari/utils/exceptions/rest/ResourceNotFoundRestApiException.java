package com.app.itibari.utils.exceptions.rest;

public class ResourceNotFoundRestApiException extends RestApiException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundRestApiException() {

        super(RestApiHttpStatus.NOT_FOUND);
    }
}
