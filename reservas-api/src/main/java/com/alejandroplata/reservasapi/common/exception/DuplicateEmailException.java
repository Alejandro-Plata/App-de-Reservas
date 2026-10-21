package com.alejandroplata.reservasapi.common.exception;

public class DuplicateEmailException extends ConflictException {
    public DuplicateEmailException() {
        super("Ya existe una cuenta con ese e-mail");
    }
}
