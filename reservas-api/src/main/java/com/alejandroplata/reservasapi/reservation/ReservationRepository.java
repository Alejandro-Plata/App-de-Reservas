package com.alejandroplata.reservasapi.reservation;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    // Dos intervalos se solapan si cada uno empieza antes de que termine el otro.
    // Con "<" y ">" estrictos, una reserva que empieza justo cuando otra acaba NO se solapa.
    @Query("""
            select count(r) > 0 from Reservation r
            where r.resource.id = :resourceId and r.status = :status
              and r.startAt < :end and r.endAt > :start
            """)
    boolean existsOverlapping(@Param("resourceId") Long resourceId, @Param("status") ReservationStatus status,
            @Param("start") Instant start, @Param("end") Instant end);

    @Query("""
            select r from Reservation r
            where r.resource.id = :resourceId and r.status = :status
              and r.startAt < :end and r.endAt > :start
            order by r.startAt
            """)
    List<Reservation> findOverlapping(@Param("resourceId") Long resourceId, @Param("status") ReservationStatus status,
            @Param("start") Instant start, @Param("end") Instant end);

    // EntityGraph trae usuario y recurso en la misma consulta: evita el problema N+1 al armar la respuesta
    @EntityGraph(attributePaths = {"user", "resource"})
    Page<Reservation> findByUserId(Long userId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"user", "resource"})
    Page<Reservation> findAll(Specification<Reservation> spec, Pageable pageable);
}
