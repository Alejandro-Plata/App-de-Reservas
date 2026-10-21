package com.alejandroplata.reservasapi.common.exception;

/** Algo que se pide por id no existe (404). */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
