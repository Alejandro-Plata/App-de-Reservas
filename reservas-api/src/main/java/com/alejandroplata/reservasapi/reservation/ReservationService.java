package com.alejandroplata.reservasapi.reservation;

import com.alejandroplata.reservasapi.common.exception.BusinessRuleException;
import com.alejandroplata.reservasapi.common.exception.ConflictException;
import com.alejandroplata.reservasapi.common.exception.ForbiddenException;
import com.alejandroplata.reservasapi.common.exception.NotFoundException;
import com.alejandroplata.reservasapi.common.exception.ReservationConflictException;
import com.alejandroplata.reservasapi.config.ReservationProperties;
import com.alejandroplata.reservasapi.reservation.dto.BusySlot;
import com.alejandroplata.reservasapi.reservation.dto.ReservationRequest;
import com.alejandroplata.reservasapi.reservation.dto.ReservationResponse;
import com.alejandroplata.reservasapi.resource.Resource;
import com.alejandroplata.reservasapi.resource.ResourceRepository;
import com.alejandroplata.reservasapi.security.UserPrincipal;
import com.alejandroplata.reservasapi.user.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final ReservationProperties props;
    private final Clock clock;

    public ReservationService(ReservationRepository reservationRepository, ResourceRepository resourceRepository,
            UserRepository userRepository, ReservationProperties props, Clock clock) {
        this.reservationRepository = reservationRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.props = props;
        this.clock = clock;
    }

    @Transactional
    public ReservationResponse create(UserPrincipal principal, ReservationRequest request) {
        validarFechas(request.startAt(), request.endAt()); // reglas 1 y 7

        Resource resource = resourceRepository.findById(request.resourceId())
                .orElseThrow(() -> new NotFoundException("Recurso " + request.resourceId() + " no encontrado"));
        if (!resource.isActive()) { // regla 5
            throw new BusinessRuleException("El recurso está inactivo y no se puede reservar");
        }

        // Regla 2. Esta comprobación da un error claro en el caso normal...
        if (reservationRepository.existsOverlapping(resource.getId(), ReservationStatus.CONFIRMED,
                request.startAt(), request.endAt())) {
            throw new ReservationConflictException();
        }

        // getReferenceById no hace SELECT: solo necesitamos el id del usuario para la FK
        Reservation reservation = new Reservation(userRepository.getReferenceById(principal.id()), resource,
                request.startAt(), request.endAt());
        try {
            // ...pero entre el "exists" y el "insert" otra petición puede colarse. Ahí es donde actúa la
            // restricción EXCLUDE de la BD (V1__init.sql): rechaza el segundo insert y lo traducimos a 409.
            // Se hace flush para que el error salte aquí y no al cerrar la transacción.
            reservationRepository.saveAndFlush(reservation);
        } catch (DataIntegrityViolationException e) {
            throw new ReservationConflictException();
        }
        return ReservationResponse.from(reservation);
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> mine(UserPrincipal principal, Pageable pageable) {
        return reservationRepository.findByUserId(principal.id(), pageable).map(ReservationResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> all(ReservationStatus status, Long resourceId, LocalDate from, LocalDate to,
            Pageable pageable) {
        // Las fechas del filtro son días completos en la zona configurada: "to" incluye todo ese día
        Instant fromInstant = from == null ? null : from.atStartOfDay(props.timeZone()).toInstant();
        Instant toInstant = to == null ? null : to.plusDays(1).atStartOfDay(props.timeZone()).toInstant();
        return reservationRepository
                .findAll(ReservationSpecifications.withFilters(status, resourceId, fromInstant, toInstant), pageable)
                .map(ReservationResponse::from);
    }

    // Reglas 3 y 6: solo el dueño o un admin; cancelar cambia el estado, no borra
    @Transactional
    public ReservationResponse cancel(UserPrincipal principal, Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Reserva " + id + " no encontrada"));
        if (!principal.isAdmin() && !reservation.getUser().getId().equals(principal.id())) {
            throw new ForbiddenException("Solo puedes cancelar tus propias reservas");
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ConflictException("La reserva ya estaba cancelada");
        }
        reservation.cancel();
        return ReservationResponse.from(reservation);
    }

    @Transactional(readOnly = true)
    public List<BusySlot> busySlots(Long resourceId, LocalDate date) {
        if (!resourceRepository.existsById(resourceId)) {
            throw new NotFoundException("Recurso " + resourceId + " no encontrado");
        }
        Instant dayStart = date.atStartOfDay(props.timeZone()).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(props.timeZone()).toInstant();
        return reservationRepository.findOverlapping(resourceId, ReservationStatus.CONFIRMED, dayStart, dayEnd)
                .stream().map(r -> new BusySlot(r.getStartAt(), r.getEndAt())).toList();
    }

    private void validarFechas(Instant start, Instant end) {
        if (!end.isAfter(start)) {
            throw new BusinessRuleException("La hora de fin debe ser posterior a la de inicio");
        }
        if (start.isBefore(clock.instant())) {
            throw new BusinessRuleException("No se puede reservar en el pasado");
        }
        long minutos = Duration.between(start, end).toMinutes();
        if (minutos < props.minMinutes()) {
            throw new BusinessRuleException("La reserva mínima es de " + props.minMinutes() + " minutos");
        }
        if (minutos > props.maxMinutes()) {
            throw new BusinessRuleException("La reserva máxima es de " + props.maxMinutes() + " minutos");
        }
    }
}
