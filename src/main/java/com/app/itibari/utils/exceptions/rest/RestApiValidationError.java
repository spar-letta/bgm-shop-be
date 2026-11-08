package com.app.itibari.utils.exceptions.rest;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RestApiValidationError {

    private String fieldName, message;
}