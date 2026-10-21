package com.alejandroplata.reservasapi.resource;

import com.alejandroplata.reservasapi.common.PageResponse;
import com.alejandroplata.reservasapi.reservation.ReservationService;
import com.alejandroplata.reservasapi.reservation.dto.BusySlot;
import com.alejandroplata.reservasapi.resource.dto.ResourceRequest;
import com.alejandroplata.reservasapi.resource.dto.ResourceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resources")
@Tag(name = "Recursos")
@SecurityRequirement(name = "bearerAuth")
public class ResourceController {

    private final ResourceService resourceService;
    private final ReservationService reservationService;

    public ResourceController(ResourceService resourceService, ReservationService reservationService) {
        this.resourceService = resourceService;
        this.reservationService = reservationService;
    }

    @Operation(summary = "Lista recursos (paginado). Filtros opcionales: name, active, category")
    @GetMapping
    public PageResponse<ResourceResponse> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) ResourceCategory category,
            @PageableDefault(sort = "name") Pageable pageable) {
        return PageResponse.of(resourceService.list(name, active, category, pageable));
    }

    @Operation(summary = "Detalle de un recurso")
    @GetMapping("/{id}")
    public ResourceResponse get(@PathVariable Long id) {
        return resourceService.get(id);
    }

    @Operation(summary = "Crea un recurso (solo ADMIN)")
    @PostMapping
    public ResponseEntity<ResourceResponse> create(@Valid @RequestBody ResourceRequest request) {
        ResourceResponse created = resourceService.create(request);
        return ResponseEntity.created(URI.create("/api/resources/" + created.id())).body(created);
    }

    @Operation(summary = "Edita un recurso (solo ADMIN)")
    @PutMapping("/{id}")
    public ResourceResponse update(@PathVariable Long id, @Valid @RequestBody ResourceRequest request) {
        return resourceService.update(id, request);
    }

    @Operation(summary = "Desactiva un recurso; no se borra (solo ADMIN)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        resourceService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Tramos ya ocupados de un recurso en un día (yyyy-MM-dd)")
    @GetMapping("/{id}/availability")
    public List<BusySlot> availability(@PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return reservationService.busySlots(id, date);
    }
}
