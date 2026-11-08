package com.app.itibari.utils.exceptions.rest;

public class SalesOrderException extends Exception {

    public SalesOrderException(String message, Throwable cause) {

        super(message, cause);
    }

    public SalesOrderException(final String message) {

        super((message));
    }
}
