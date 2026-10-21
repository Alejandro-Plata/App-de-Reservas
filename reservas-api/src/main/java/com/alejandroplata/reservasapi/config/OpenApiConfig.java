package com.alejandroplata.reservasapi.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

// Con este esquema Swagger UI muestra el botón "Authorize" para pegar el token del login
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Reservas API",
        version = "1.0",
        description = "API REST para reservar recursos (salas, pistas...) sin solapamientos. "
                + "Flujo: POST /api/auth/register o /login, copia el token, pulsa Authorize y prueba el resto."))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
