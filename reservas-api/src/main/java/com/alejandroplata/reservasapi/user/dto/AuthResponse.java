package com.alejandroplata.reservasapi.user.dto;

import com.alejandroplata.reservasapi.user.Role;

public record AuthResponse(String token, String email, Role role) {
}
