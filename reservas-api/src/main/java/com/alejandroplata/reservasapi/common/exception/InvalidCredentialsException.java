package com.alejandroplata.reservasapi.common.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        // Mismo mensaje si falla el e-mail o la contraseña: no revelamos qué cuentas existen
        super("E-mail o contraseña incorrectos");
    }
}
