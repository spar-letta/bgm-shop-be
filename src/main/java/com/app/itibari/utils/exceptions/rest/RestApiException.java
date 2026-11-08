package com.app.itibari.utils.exceptions.rest;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class RestApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final RestApiError restApiError;

    public RestApiException(RestApiError restApiError) {

        this.restApiError = restApiError;
    }

    public RestApiException(RestApiHttpStatus httpStatus) {

        this.restApiError = new RestApiError(httpStatus);
    }

    public RestApiException apiCode(UUID apiCode) {

        this.restApiError.setApiCode(apiCode);
        return this;
    }

    public RestApiException userMessage(String userMessage) {

        this.restApiError.setUserMessage(userMessage);
        return this;
    }

    public RestApiException userMessage(String userMessage, Object... userMessageArgs) {

        this.userMessage(String.format(userMessage, userMessageArgs));
        return this;
    }

    public RestApiException developerMessage(String developerMessage) {

        this.restApiError.setDeveloperMessage(developerMessage);
        return this;
    }

    public RestApiException developerMessage(String developerMessage, Object... developerMessageArgs) {

        this.developerMessage(String.format(developerMessage, developerMessageArgs));
        return this;
    }

    public RestApiException validationErrors(List<RestApiValidationError> validationErrors) {

        this.restApiError.setValidationErrors(validationErrors);
        return this;
    }

    public RestApiException validationErrors(RestApiValidationError... validationErrors) {

        this.restApiError.setValidationErrors(Arrays.asList(validationErrors));
        return this;
    }

    public RestApiError getRestApiError() {

        return restApiError;
    }

}
