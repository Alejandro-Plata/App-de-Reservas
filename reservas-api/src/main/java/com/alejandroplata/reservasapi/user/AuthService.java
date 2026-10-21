package com.alejandroplata.reservasapi.user;

import com.alejandroplata.reservasapi.common.exception.DuplicateEmailException;
import com.alejandroplata.reservasapi.common.exception.InvalidCredentialsException;
import com.alejandroplata.reservasapi.security.JwtService;
import com.alejandroplata.reservasapi.user.dto.AuthResponse;
import com.alejandroplata.reservasapi.user.dto.LoginRequest;
import com.alejandroplata.reservasapi.user.dto.RegisterRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizar(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }
        // El rol nunca viene del cliente: quien se registra siempre es USER
        User user = userRepository.save(new User(email, passwordEncoder.encode(request.password()), Role.USER));
        return respuesta(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizar(request.email()))
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return respuesta(user);
    }

    private AuthResponse respuesta(User user) {
        return new AuthResponse(jwtService.generate(user), user.getEmail(), user.getRole());
    }

    // "Ana@Correo.com " y "ana@correo.com" son la misma cuenta
    static String normalizar(String email) {
        return email.trim().toLowerCase();
    }
}
