package com.alejandroplata.reservasapi.reservation;

import com.alejandroplata.reservasapi.resource.Resource;
import com.alejandroplata.reservasapi.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LAZY: no cargamos el usuario/recurso hasta que se usan (evita joins innecesarios)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    // Instant + timestamptz: un instante absoluto, sin ambigüedad de zona horaria ni cambios de hora
    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    protected Reservation() {
    }

    public Reservation(User user, Resource resource, Instant startAt, Instant endAt) {
        this.user = user;
        this.resource = resource;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = ReservationStatus.CONFIRMED;
    }

    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Resource getResource() { return resource; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    public ReservationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
