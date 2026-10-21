package com.alejandroplata.reservasapi.common.exception;

/** El hueco pedido ya tiene una reserva confirmada. */
public class ReservationConflictException extends ConflictException {
    public ReservationConflictException() {
        super("El recurso ya está reservado en ese tramo horario");
    }
}
