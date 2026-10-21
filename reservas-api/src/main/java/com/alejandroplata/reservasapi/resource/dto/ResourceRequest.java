package com.alejandroplata.reservasapi.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.alejandroplata.reservasapi.resource.ResourceCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ResourceRequest(
        @Schema(example = "Sala Mario") @NotBlank @Size(max = 100) String name,
        @Schema(example = "Sala de reuniones con pantalla") @Size(max = 500) String description,
        @Schema(example = "SALA") @NotNull ResourceCategory category,
        @Schema(example = "8") @Positive int capacity) {
}
