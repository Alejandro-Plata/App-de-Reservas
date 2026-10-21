package com.alejandroplata.reservasapi.common.exception;

/** El usuario está autenticado pero no es dueño del recurso que intenta tocar (403). */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
