package com.alejandroplata.reservasapi.resource;

import com.alejandroplata.reservasapi.common.exception.NotFoundException;
import com.alejandroplata.reservasapi.resource.dto.ResourceRequest;
import com.alejandroplata.reservasapi.resource.dto.ResourceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceService {

    private final ResourceRepository repository;

    public ResourceService(ResourceRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<ResourceResponse> list(String name, Boolean active, ResourceCategory category, Pageable pageable) {
        // Cada filtro que no llega devuelve null y simplemente no restringe la consulta
        Specification<Resource> spec = (root, query, cb) ->
                name == null || name.isBlank() ? null
                        : cb.like(cb.lower(root.get("name")), "%" + name.trim().toLowerCase() + "%");
        spec = spec.and((root, query, cb) -> active == null ? null : cb.equal(root.get("active"), active));
        spec = spec.and((root, query, cb) -> category == null ? null : cb.equal(root.get("category"), category));
        return repository.findAll(spec, pageable).map(ResourceResponse::from);
    }

    @Transactional(readOnly = true)
    public ResourceResponse get(Long id) {
        return ResourceResponse.from(find(id));
    }

    @Transactional
    public ResourceResponse create(ResourceRequest request) {
        Resource resource = new Resource(request.name().trim(), descripcion(request), request.category(), request.capacity());
        return ResourceResponse.from(repository.save(resource));
    }

    @Transactional
    public ResourceResponse update(Long id, ResourceRequest request) {
        Resource resource = find(id);
        resource.update(request.name().trim(), descripcion(request), request.category(), request.capacity());
        return ResourceResponse.from(resource); // al cerrar la transacción JPA guarda el cambio solo (dirty checking)
    }

    // Desactivar, no borrar: las reservas históricas siguen apuntando a este recurso
    @Transactional
    public void deactivate(Long id) {
        find(id).deactivate();
    }

    private Resource find(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Recurso " + id + " no encontrado"));
    }

    private String descripcion(ResourceRequest request) {
        return request.description() == null ? "" : request.description().trim();
    }
}
