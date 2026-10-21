package com.alejandroplata.reservasapi.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "ana@correo.com") @NotBlank String email,
        @Schema(example = "contraseña-segura") @NotBlank String password) {
}
