package com.alejandroplata.reservasapi.common;

import java.util.List;
import org.springframework.data.domain.Page;

/** Página con solo lo que necesita el cliente (no serializamos PageImpl directamente). */
public record PageResponse<T>(List<T> content, long totalElements, int totalPages, int number, int size) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getTotalElements(), page.getTotalPages(),
                page.getNumber(), page.getSize());
    }
}
