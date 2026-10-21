package com.alejandroplata.reservasapi.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Schema(example = "ana@correo.com") @NotBlank @Email @Size(max = 255) String email,
        // 72 es el máximo que BCrypt tiene en cuenta (bytes); más allá se ignora en silencio
        @Schema(example = "contraseña-segura") @NotBlank @Size(min = 8, max = 72) String password) {
}
