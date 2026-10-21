package com.alejandroplata.reservasapi.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alejandroplata.reservasapi.common.exception.BusinessRuleException;
import com.alejandroplata.reservasapi.common.exception.ConflictException;
import com.alejandroplata.reservasapi.common.exception.ForbiddenException;
import com.alejandroplata.reservasapi.common.exception.NotFoundException;
import com.alejandroplata.reservasapi.common.exception.ReservationConflictException;
import com.alejandroplata.reservasapi.config.ReservationProperties;
import com.alejandroplata.reservasapi.reservation.dto.ReservationRequest;
import com.alejandroplata.reservasapi.reservation.dto.ReservationResponse;
import com.alejandroplata.reservasapi.resource.Resource;
import com.alejandroplata.reservasapi.resource.ResourceCategory;
import com.alejandroplata.reservasapi.resource.ResourceRepository;
import com.alejandroplata.reservasapi.security.UserPrincipal;
import com.alejandroplata.reservasapi.user.Role;
import com.alejandroplata.reservasapi.user.User;
import com.alejandroplata.reservasapi.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    // "Ahora" fijo para que los tests no dependan de la hora real
    private static final Instant NOW = Instant.parse("2030-01-01T10:00:00Z");

    @Mock ReservationRepository reservationRepository;
    @Mock ResourceRepository resourceRepository;
    @Mock UserRepository userRepository;

    ReservationService service;
    Resource resource;
    User owner;
    UserPrincipal ownerPrincipal;

    @BeforeEach
    void setUp() {
        var props = new ReservationProperties(30, 240, ZoneId.of("Europe/Madrid"));
        service = new ReservationService(reservationRepository, resourceRepository, userRepository, props,
                Clock.fixed(NOW, ZoneOffset.UTC));

        resource = new Resource("Sala Mario", "", ResourceCategory.SALA, 8);
        ReflectionTestUtils.setField(resource, "id", 1L);
        owner = new User("ana@correo.com", "hash", Role.USER);
        ReflectionTestUtils.setField(owner, "id", 10L);
        ownerPrincipal = new UserPrincipal(10L, "ana@correo.com", Role.USER);
    }

    private ReservationRequest request(String start, String end) {
        return new ReservationRequest(1L, Instant.parse(start), Instant.parse(end));
    }

    private void recursoYUsuarioExisten() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
        when(userRepository.getReferenceById(10L)).thenReturn(owner);
    }

    @Test
    @DisplayName("crea una reserva confirmada cuando todo es válido y el hueco está libre")
    void creaReservaValida() {
        recursoYUsuarioExisten();
        when(reservationRepository.existsOverlapping(any(), any(), any(), any())).thenReturn(false);

        ReservationResponse res = service.create(ownerPrincipal, request("2030-01-02T10:00:00Z", "2030-01-02T11:00:00Z"));

        assertThat(res.status()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(res.resourceName()).isEqualTo("Sala Mario");
        assertThat(res.userEmail()).isEqualTo("ana@correo.com");
        verify(reservationRepository).saveAndFlush(any(Reservation.class));
    }

    @Test
    @DisplayName("rechaza una reserva cuyo fin no es posterior al inicio")
    void rechazaFinAnteriorAlInicio() {
        assertThatThrownBy(() -> service.create(ownerPrincipal, request("2030-01-02T11:00:00Z", "2030-01-02T10:00:00Z")))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("posterior");
        verify(reservationRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("rechaza reservar en el pasado")
    void rechazaPasado() {
        assertThatThrownBy(() -> service.create(ownerPrincipal, request("2029-12-31T10:00:00Z", "2029-12-31T11:00:00Z")))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("pasado");
    }

    @Test
    @DisplayName("rechaza reservas más cortas que la duración mínima")
    void rechazaDemasiadoCorta() {
        assertThatThrownBy(() -> service.create(ownerPrincipal, request("2030-01-02T10:00:00Z", "2030-01-02T10:10:00Z")))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("mínima");
    }

    @Test
    @DisplayName("rechaza reservas más largas que la duración máxima")
    void rechazaDemasiadoLarga() {
        assertThatThrownBy(() -> service.create(ownerPrincipal, request("2030-01-02T10:00:00Z", "2030-01-02T16:00:00Z")))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("máxima");
    }

    @Test
    @DisplayName("rechaza reservar un recurso inexistente")
    void rechazaRecursoInexistente() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(ownerPrincipal, request("2030-01-02T10:00:00Z", "2030-01-02T11:00:00Z")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("rechaza reservar un recurso inactivo")
    void rechazaRecursoInactivo() {
        resource.deactivate();
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));

        assertThatThrownBy(() -> service.create(ownerPrincipal, request("2030-01-02T10:00:00Z", "2030-01-02T11:00:00Z")))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("inactivo");
        verify(reservationRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("rechaza con conflicto cuando el tramo se solapa con otra reserva confirmada")
    void rechazaSolapamiento() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
        when(reservationRepository.existsOverlapping(any(), any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.create(ownerPrincipal, request("2030-01-02T10:00:00Z", "2030-01-02T11:00:00Z")))
                .isInstanceOf(ReservationConflictException.class);
        verify(reservationRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("traduce a conflicto el error de la BD cuando otra petición ocupó el hueco a la vez")
    void traduceViolacionDeLaRestriccionDeExclusion() {
        recursoYUsuarioExisten();
        when(reservationRepository.existsOverlapping(any(), any(), any(), any())).thenReturn(false);
        when(reservationRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("ex_reservations_no_overlap"));

        assertThatThrownBy(() -> service.create(ownerPrincipal, request("2030-01-02T10:00:00Z", "2030-01-02T11:00:00Z")))
                .isInstanceOf(ReservationConflictException.class);
    }

    private Reservation reservaDeAna() {
        Reservation r = new Reservation(owner, resource, Instant.parse("2030-01-02T10:00:00Z"), Instant.parse("2030-01-02T11:00:00Z"));
        ReflectionTestUtils.setField(r, "id", 5L);
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(r));
        return r;
    }

    @Test
    @DisplayName("el dueño puede cancelar su reserva y pasa a CANCELLED sin borrarse")
    void duenoCancela() {
        Reservation r = reservaDeAna();

        ReservationResponse res = service.cancel(ownerPrincipal, 5L);

        assertThat(res.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        verify(reservationRepository, never()).delete(any(Reservation.class));
    }

    @Test
    @DisplayName("un usuario no puede cancelar la reserva de otro")
    void noCancelaAjena() {
        reservaDeAna();
        var otro = new UserPrincipal(99L, "otro@correo.com", Role.USER);

        assertThatThrownBy(() -> service.cancel(otro, 5L)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("un admin puede cancelar la reserva de cualquiera")
    void adminCancelaAjena() {
        reservaDeAna();
        var admin = new UserPrincipal(1L, "admin@correo.com", Role.ADMIN);

        assertThat(service.cancel(admin, 5L).status()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancelar una reserva ya cancelada devuelve conflicto")
    void noCancelaDosVeces() {
        Reservation r = reservaDeAna();
        r.cancel();

        assertThatThrownBy(() -> service.cancel(ownerPrincipal, 5L)).isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("cancelar una reserva inexistente devuelve not found")
    void cancelarInexistente() {
        when(reservationRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(ownerPrincipal, 5L)).isInstanceOf(NotFoundException.class);
    }
}
