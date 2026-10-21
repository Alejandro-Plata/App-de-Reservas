package com.alejandroplata.reservasapi.resource.dto;

import com.alejandroplata.reservasapi.resource.Resource;
import com.alejandroplata.reservasapi.resource.ResourceCategory;

// Se devuelve este DTO y no la entidad: así la API no queda atada a las columnas ni a las relaciones JPA
public record ResourceResponse(Long id, String name, String description, ResourceCategory category, int capacity, boolean active) {

    public static ResourceResponse from(Resource r) {
        return new ResourceResponse(r.getId(), r.getName(), r.getDescription(), r.getCategory(), r.getCapacity(), r.isActive());
    }
}
