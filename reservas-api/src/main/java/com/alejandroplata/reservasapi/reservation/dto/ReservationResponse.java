package com.alejandroplata.reservasapi.reservation.dto;

import com.alejandroplata.reservasapi.reservation.Reservation;
import com.alejandroplata.reservasapi.reservation.ReservationStatus;
import java.time.Instant;

public record ReservationResponse(Long id, Long resourceId, String resourceName, String userEmail,
        Instant startAt, Instant endAt, ReservationStatus status) {

    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(r.getId(), r.getResource().getId(), r.getResource().getName(),
                r.getUser().getEmail(), r.getStartAt(), r.getEndAt(), r.getStatus());
    }
}
