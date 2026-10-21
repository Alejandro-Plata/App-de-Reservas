package com.alejandroplata.reservasapi.security;

import com.alejandroplata.reservasapi.user.Role;

/** Quién hace la petición, extraído del token (sin consultar la BD en cada request). */
public record UserPrincipal(Long id, String email, Role role) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
