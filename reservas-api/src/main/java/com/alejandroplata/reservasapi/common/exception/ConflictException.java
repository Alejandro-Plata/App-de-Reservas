package com.alejandroplata.reservasapi.common.exception;

/** El estado actual impide la operación (409). */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
