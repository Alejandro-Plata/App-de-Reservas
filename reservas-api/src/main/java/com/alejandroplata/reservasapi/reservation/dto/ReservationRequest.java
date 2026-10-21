package com.alejandroplata.reservasapi.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record ReservationRequest(
        @Schema(example = "1") @NotNull Long resourceId,
        @Schema(example = "2030-06-01T10:00:00Z", description = "Instante ISO-8601 con zona") @NotNull Instant startAt,
        @Schema(example = "2030-06-01T11:00:00Z") @NotNull Instant endAt) {
}
