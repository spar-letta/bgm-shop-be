package com.app.itibari.utils.exceptions.rest;

import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CommonRestControllerAdvice {

    protected final static String JSON = "application/json";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected ResponseEntity<RestApiError> createResponseEntity(RestApiError restApiError) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpStatus responseStatus = HttpStatus.valueOf(restApiError.getHttpStatus());
        ResponseEntity<RestApiError> result = new ResponseEntity<>(restApiError, headers, responseStatus);
        return result;
    }

    @ExceptionHandler(RestApiException.class)
    public ResponseEntity<RestApiError> handleRestApiException(RestApiException e) {

        logger.error("Api Error caused by exception", e);
        return this.createResponseEntity(e.getRestApiError());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestApiError> handleException(Exception e) {

        logger.error("Api Error caused by exception", e);
        RestApiError restApiError = new RestApiError(RestApiHttpStatus.INTERNAL_SERVER_ERROR);
        restApiError.setApiCode(ApiErrorCodes.UNHANDLED_SERVER_EXCEPTION);
        restApiError.setUserMessage("The server encountered an error and could not complete the request");
        restApiError.setDeveloperMessage("The server encountered an unhandled exception and could not complete the request");

        return createResponseEntity(restApiError);
    }

    /**
     * According to the SpringMVC documentation. A @PathVariable argument can be of any simple type such as int, long, Date, etc. Spring
     * automatically converts to the appropriate type or throws a TypeMismatchException if it fails to do so
     *
     * @param e
     * @return
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<RestApiError> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e) {

        logger.error("Api Error caused by exception", e);
        RestApiError restApiError = new RestApiError(RestApiHttpStatus.METHOD_NOT_ALLOWED);
        restApiError.setApiCode(ApiErrorCodes.INVALID_REQUEST_BODY);
        restApiError.setUserMessage("HTTP method " + e.getMethod() + " is not supported for this URL");
        restApiError.setDeveloperMessage(e.getMessage());

        return createResponseEntity(restApiError);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<RestApiError> handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException e) {

        logger.error("Api Error caused by exception", e);
        RestApiError restApiError = new RestApiError(RestApiHttpStatus.UNSUPPORTED_MEDIA_TYPE);
        restApiError.setApiCode(ApiErrorCodes.UNABLE_TO_PARSE_REQUEST);
        restApiError.setUserMessage("This Content-Type is not supported for this URL");
        restApiError.setDeveloperMessage(e.getMessage());

        return createResponseEntity(restApiError);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<RestApiError> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e) {

        logger.error("Api Error caused by exception", e);
        RestApiError restApiError = new RestApiError(RestApiHttpStatus.BAD_REQUEST);
        restApiError.setApiCode(ApiErrorCodes.INVALID_REQUEST_BODY);
        restApiError.setUserMessage("An Error has occured and the request could not be completed");
        restApiError.setDeveloperMessage(e.getMessage());

        return createResponseEntity(restApiError);
    }

    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<RestApiError> handleTypeMismatchException(TypeMismatchException e) {

        logger.error("Api Error caused by exception", e);
        RestApiError restApiError = new RestApiError(RestApiHttpStatus.BAD_REQUEST);
        restApiError.setApiCode(ApiErrorCodes.TYPE_MISMATCH_EXCEPTION);
        restApiError.setUserMessage("An Error has occured and the request could not be completed");
        restApiError.setDeveloperMessage(e.getMessage());

        return createResponseEntity(restApiError);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<RestApiError> handleAccessDeniedException(AccessDeniedException e) {
        //logger.info("AD Api Error caused by exception", e);
        RestApiError restApiError = new RestApiError(RestApiHttpStatus.UNAUTHORIZED);
        restApiError.setApiCode(ApiErrorCodes.ACCESS_DENIED);
        restApiError.setUserMessage("You do not have the privilege to access this resource.");
        restApiError.setDeveloperMessage(e.getMessage());

        return createResponseEntity(restApiError);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RestApiError> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {

        logger.error("Api Error caused by exception", e);
        RestApiError restApiError = new RestApiError(RestApiHttpStatus.BAD_REQUEST);
        restApiError.setApiCode(ApiErrorCodes.UNABLE_TO_PARSE_REQUEST);
        restApiError.setUserMessage("An Error has occured and the request could not be completed");
        restApiError.setDeveloperMessage(e.getMessage());

        return createResponseEntity(restApiError);
    }

    @ExceptionHandler(ClientAbortException.class)
    public ResponseEntity<RestApiError> handleClientAbortException(ClientAbortException e) {

        return null;
    }

    /**
     * According to the SpringMVC documentation @RequestBody validation errors always result in a MethodArgumentNotValidException being
     * raised. The exception is handled in the DefaultHandlerExceptionResolver, which sends a 400 error back to the client. We are defining
     * a handler here to send back a RestAPiError.
     *
     * @param e
     * @return
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RestApiError> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {

        logger.error("Api Error caused by exception", e);
        BindingResult bindingResult = e.getBindingResult();
        return restApiErrorFromBindingResult(bindingResult, ApiErrorCodes.INVALID_REQUEST_BODY);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<RestApiError> handleBindException(BindException e) {

        logger.error("Api Error caused by exception", e);
        return restApiErrorFromBindingResult(e, ApiErrorCodes.BIND_EXCEPTION);
    }

    private ResponseEntity<RestApiError> restApiErrorFromBindingResult(BindingResult bindingResult, UUID apiErrorCode) {
        // Create an Api Error and return it
        RestApiError restApiError = new RestApiError(RestApiHttpStatus.BAD_REQUEST);
        restApiError.setApiCode(apiErrorCode);
        restApiError.setUserMessage("An Error has occured and the request could not be completed");
        restApiError.setDeveloperMessage("Invalid request templates.body see validation errors");

        // Extract field errors
        List<FieldError> fieldErrors = bindingResult.getFieldErrors();
        List<RestApiValidationError> restApiFieldErrors = new ArrayList<>();
        for (FieldError fieldError : fieldErrors) {
            RestApiValidationError restApiFieldError = new RestApiValidationError();
            restApiFieldError.setFieldName(fieldError.getField());
            restApiFieldError.setMessage(fieldError.getDefaultMessage());
            restApiFieldErrors.add(restApiFieldError);
        }
        if (!restApiFieldErrors.isEmpty()) {
            restApiError.setValidationErrors(restApiFieldErrors);
        }

        return createResponseEntity(restApiError);
    }
}
