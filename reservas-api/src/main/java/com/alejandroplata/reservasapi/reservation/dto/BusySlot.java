package com.alejandroplata.reservasapi.reservation.dto;

import java.time.Instant;

/** Tramo ya ocupado de un recurso. No incluye quién reservó (dato privado). */
public record BusySlot(Instant startAt, Instant endAt) {
}
