package com.alejandroplata.reservasapi.reservation;

import com.alejandroplata.reservasapi.common.PageResponse;
import com.alejandroplata.reservasapi.reservation.dto.ReservationRequest;
import com.alejandroplata.reservasapi.reservation.dto.ReservationResponse;
import com.alejandroplata.reservasapi.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Reservas")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {

    private final ReservationService service;

    public ReservationController(ReservationService service) {
        this.service = service;
    }

    @Operation(summary = "Crea una reserva. 409 si el tramo ya está ocupado, 422 si rompe una regla")
    @PostMapping
    public ResponseEntity<ReservationResponse> create(@AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReservationRequest request) {
        ReservationResponse created = service.create(principal, request);
        return ResponseEntity.created(URI.create("/api/reservations/" + created.id())).body(created);
    }

    @Operation(summary = "Mis reservas (paginado)")
    @GetMapping("/me")
    public PageResponse<ReservationResponse> mine(@AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(sort = "startAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.of(service.mine(principal, pageable));
    }

    @Operation(summary = "Todas las reservas con filtros (solo ADMIN). Fechas yyyy-MM-dd")
    @GetMapping
    public PageResponse<ReservationResponse> all(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(sort = "startAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.of(service.all(status, resourceId, from, to, pageable));
    }

    @Operation(summary = "Cancela una reserva (dueño o ADMIN)")
    @PatchMapping("/{id}/cancel")
    public ReservationResponse cancel(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return service.cancel(principal, id);
    }
}
