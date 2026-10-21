package com.alejandroplata.reservasapi.reservation;

import java.time.Instant;
import org.springframework.data.jpa.domain.Specification;

// Cada método devuelve una condición opcional; si el filtro es null no restringe nada
final class ReservationSpecifications {

    private ReservationSpecifications() {
    }

    static Specification<Reservation> withFilters(ReservationStatus status, Long resourceId, Instant from, Instant to) {
        return Specification.<Reservation>unrestricted()
                .and((root, q, cb) -> status == null ? null : cb.equal(root.get("status"), status))
                .and((root, q, cb) -> resourceId == null ? null : cb.equal(root.get("resource").get("id"), resourceId))
                .and((root, q, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("startAt"), from))
                .and((root, q, cb) -> to == null ? null : cb.lessThan(root.get("startAt"), to));
    }
}
