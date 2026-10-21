package com.alejandroplata.reservasapi.common.exception;

/** La petición está bien formada pero rompe una regla de negocio (422). */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
